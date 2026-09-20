#!/usr/bin/env python3
"""Verify every external service WurieAI depends on, without ever printing a secret.

Each service gets a status:

  ok    connected and verified
  warn  connected, but something in the repo/config does not line up
  fail  configured, but the connection does not work
  skip  no credentials available yet (the fix line says how to get them)

Only key *names* are ever printed. Values are never echoed, logged or written back.

Prefer the shell entry point, which also collects the GitHub evidence:

  bash tools/check-services.sh

Environment is loaded from ``.env`` / ``.env.local`` (repo root) and
``wurie-backend/.env`` when present, the same files the app and the shells use. Pass
``--no-env`` to use only the ambient environment.

Usage:
  python3 tools/check-services.py
  python3 tools/check-services.py --only render,backend
  python3 tools/check-services.py --json
  python3 tools/check-services.py --offline      # repo/config checks only, no network
  python3 tools/check-services.py --strict       # a skip also counts as failure (CI gate)

Exit codes: 0 = every configured service connected, 1 = at least one failure.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass, field, asdict
from pathlib import Path

try:  # Python 3.11+ standard library
    import tomllib
except ModuleNotFoundError:  # pragma: no cover - not needed by this script
    tomllib = None  # type: ignore[assignment]

REPO_ROOT = Path(__file__).resolve().parent.parent

DEFAULT_BACKEND_URL = "https://wurieai-superapp.onrender.com"
DEFAULT_WEB_URL = "https://wurieai-web.vercel.app"
GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"

# Flat string contract that /health must expose (see wurie-backend/README.md).
HEALTH_CONTRACT = (
    "status",
    "agent_available",
    "agent_model",
    "agent_fallback",
    "agent_reason",
    "firebase_admin",
    "storage",
    "error_tracking",
    "auth_mode",
    "app_check",
)

STATUS_ORDER = {"fail": 0, "warn": 1, "ok": 2, "skip": 3}

# Anything that looks like a credential, in case a CLI prints one despite our best efforts.
_SECRET_PATTERNS = (
    re.compile(r"\b(gh[pousr]_[A-Za-z0-9]{16,})\b"),
    re.compile(r"\b(AIza[0-9A-Za-z_\-]{20,})\b"),
    re.compile(r"\b(sk-[A-Za-z0-9]{16,})\b"),
    re.compile(r"\b(rnd_[A-Za-z0-9]{16,})\b"),
    re.compile(r"\b(sntrys_[A-Za-z0-9]{16,})\b"),
    re.compile(r"\b(lsv2_[A-Za-z0-9]{16,})\b"),
    re.compile(r"\b([A-Za-z0-9_\-]{40,})\b"),
)


def redact(text: str) -> str:
    """Strip anything credential-shaped out of text we are about to print."""
    for pattern in _SECRET_PATTERNS:
        text = pattern.sub("<redacted>", text)
    return text


@dataclass
class Result:
    service: str
    status: str
    detail: str
    fix: str = ""
    env: dict = field(default_factory=dict)


@dataclass
class Context:
    offline: bool = False
    backend_url: str = DEFAULT_BACKEND_URL
    web_url: str = DEFAULT_WEB_URL
    render_service: str = ""
    repo_slug: str = ""
    gh_dir: str = ""


def env_state(names) -> dict:
    """Which of these keys are set. Values are never read into the output."""
    return {name: ("set" if os.environ.get(name) else "missing") for name in names}


def run(cmd, timeout=25, env=None):
    """Run a CLI, returning (returncode, sanitized output)."""
    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout,
            env={**os.environ, **(env or {})},
        )
    except FileNotFoundError:
        return 127, "command not found"
    except subprocess.TimeoutExpired:
        return 124, "timed out"
    output = (proc.stdout or "") + (proc.stderr or "")
    return proc.returncode, redact(output.strip())


def http(url, headers=None, timeout=20):
    """GET a URL, returning (status_code, body). Never raises for HTTP errors."""
    request = urllib.request.Request(url, headers=headers or {})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read(200_000).decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        return error.code, error.read(4_000).decode("utf-8", "replace")
    except Exception as error:  # noqa: BLE001 - any transport failure is reported, not raised
        return None, f"{type(error).__name__}: {error}"


def first_line(text: str, limit: int = 140) -> str:
    for line in text.splitlines():
        line = line.strip()
        if line:
            return line[:limit]
    return ""


def evidence(ctx: Context, name: str):
    """Read a file collected by tools/check-services.sh, or None when absent.

    Freebuff attaches its repository credential to direct ``gh`` calls only, so nested
    invocations from this process may be unauthenticated. The shell wrapper runs the
    ``gh`` probes where the credential does apply and hands the raw JSON over here.
    """
    if not ctx.gh_dir:
        return None
    path = Path(ctx.gh_dir) / name
    if not path.is_file():
        return ""
    try:
        return path.read_text()
    except OSError:
        return ""


GH_EVIDENCE_FIX = ("GitHub probes need a gh invocation in the same command, which is where "
                   "Freebuff attaches the repository credential. On a developer machine just run "
                   "`gh auth login` once; in this workspace run "
                   "`gh --version >/dev/null; bash tools/check-services.sh`")


# --------------------------------------------------------------------------- github


def workflow_secrets_referenced() -> set:
    referenced = set()
    workflows = REPO_ROOT / ".github" / "workflows"
    if workflows.is_dir():
        for path in sorted(workflows.glob("*.y*ml")):
            referenced.update(re.findall(r"secrets\.([A-Z0-9_]+)", path.read_text()))
    return referenced


def check_github(ctx: Context) -> Result:
    """Probe with a real API call, never with `gh auth status`.

    Freebuff injects a repository-scoped credential per command, so a persisted login is
    not required and its absence proves nothing. Only an actual API call does.
    """
    names = ("GH_TOKEN", "GITHUB_TOKEN")
    state = env_state(names)
    if not shutil.which("gh"):
        return Result("github", "skip", "gh CLI not installed",
                      "bash tools/clis/install.sh", state)

    collected = evidence(ctx, "repo.json")
    if collected is not None:
        if not collected.strip():
            return Result("github", "skip",
                          first_line(evidence(ctx, "repo.err") or "the repository is not readable"),
                          "reconnect the repository in Freebuff, or update the Freebuff GitHub App "
                          "permissions (never paste a personal access token)", state)
        try:
            payload = json.loads(collected)
            ctx.repo_slug = payload.get("nameWithOwner", "")
            branch = (payload.get("defaultBranchRef") or {}).get("name", "")
        except json.JSONDecodeError:
            ctx.repo_slug, branch = "", ""
    else:
        code, output = run(["gh", "repo", "view", "--json", "nameWithOwner,defaultBranchRef"], timeout=25)
        if code != 0:
            return Result("github", "skip",
                          first_line(output) or "the repository is not readable",
                          GH_EVIDENCE_FIX, state)
        try:
            payload = json.loads(output)
            ctx.repo_slug = payload.get("nameWithOwner", "")
            branch = (payload.get("defaultBranchRef") or {}).get("name", "")
        except json.JSONDecodeError:
            ctx.repo_slug, branch = "", ""
    detail = f"repo {ctx.repo_slug or '?'}"
    if branch:
        detail += f" (default {branch})"

    # Workflow-referenced secrets. App installation tokens cannot read Actions secrets
    # (HTTP 403), which is expected and is not a connection failure.
    referenced = workflow_secrets_referenced()
    if referenced:
        secrets_text = evidence(ctx, "secrets.txt") if ctx.gh_dir else None
        if secrets_text is None:
            code, secrets_text = run(["gh", "secret", "list"], timeout=25)
            if code != 0:
                secrets_text = "" if "403" in secrets_text else None
        if secrets_text:
            defined = {line.split("\t")[0].strip() for line in secrets_text.splitlines() if line.strip()}
            missing = sorted(referenced - defined)
            if missing:
                return Result("github", "warn",
                              f"{detail}; workflow secrets missing: {', '.join(missing)}",
                              "the affected workflow jobs cannot run until these are set", state)
            detail += f"; {len(referenced)} workflow secrets defined"
        else:
            detail += f"; {len(referenced)} workflow secrets referenced (not readable with app credentials)"
    return Result("github", "ok", detail, env=state)


def check_ci(ctx: Context) -> Result:
    """Report the latest GitHub Actions run per workflow, and why the red ones failed."""
    collected = evidence(ctx, "runs.json") if ctx.gh_dir else None
    if collected is not None:
        output = collected
    else:
        if not shutil.which("gh"):
            return Result("ci", "skip", "gh CLI not installed", "bash tools/clis/install.sh")
        code, output = run([
            "gh", "run", "list", "--limit", "40", "--json",
            "workflowName,status,conclusion,headBranch,createdAt,databaseId",
        ], timeout=40)
        if code != 0:
            return Result("ci", "skip", first_line(output) or "run list unavailable", GH_EVIDENCE_FIX)
    if not output.strip():
        return Result("ci", "skip", "no run history collected", GH_EVIDENCE_FIX)
    try:
        runs = json.loads(output)
    except json.JSONDecodeError:
        return Result("ci", "warn", "unparseable run list", first_line(output))

    latest = {}
    for item in runs:
        name = item.get("workflowName") or "?"
        latest.setdefault(name, item)
    failed = {name: run_ for name, run_ in latest.items() if run_.get("conclusion") == "failure"}
    summary = ", ".join(
        f"{name}: {run_.get('conclusion') or run_.get('status')}" for name, run_ in sorted(latest.items())
    )
    if not failed:
        return Result("ci", "ok", summary or "no runs yet")

    reasons = []
    for name, run_ in sorted(failed.items()):
        reason = ""
        run_id = str(run_.get("databaseId", ""))
        if run_id and ctx.gh_dir:
            reason = first_line(evidence(ctx, f"run-{run_id}.txt") or "", 120)
        elif run_id:
            code, jobs_output = run([
                "gh", "run", "view", run_id, "--json", "jobs",
                "--jq", "[.jobs[] | select(.conclusion == \"failure\") | .name + \" / \" + "
                        "([.steps[]? | select(.conclusion == \"failure\") | .name] | join(\", \"))] | join(\"; \")",
            ], timeout=40)
            if code == 0:
                reason = first_line(jobs_output, 120)
        reasons.append(f"{name}: {reason or 'see the run log'}")
    return Result("ci", "warn", f"failing workflows -> {' | '.join(reasons)}",
                  "read `gh run view <id> --log-failed` for the full step output")


# --------------------------------------------------------------------------- render


def render_service_from_blueprint() -> str:
    path = REPO_ROOT / "render.yaml"
    if not path.is_file():
        return ""
    text = path.read_text()
    match = re.search(r"^\s*-?\s*name:\s*([A-Za-z0-9\-_.]+)\s*$", text, re.MULTILINE)
    return match.group(1) if match else ""


def check_render(ctx: Context) -> Result:
    names = ("RENDER_API_KEY", "RENDER_DEPLOY_HOOK_URL")
    state = env_state(names)
    ctx.render_service = render_service_from_blueprint()

    if not shutil.which("render"):
        return Result("render", "skip", "render CLI not installed",
                      "bash tools/clis/install.sh   (or ONLY=render ...)", state)
    if state["RENDER_API_KEY"] == "missing":
        return Result("render", "skip", "RENDER_API_KEY not set (no CLI session)",
                      "Render dashboard -> Account Settings -> API Keys, then export RENDER_API_KEY",
                      state)

    code, output = run(["render", "whoami", "-o", "json"])
    if code != 0:
        return Result("render", "fail", first_line(output) or "whoami failed",
                      "check the RENDER_API_KEY value and workspace access", state)
    try:
        who = json.loads(output)
        owner = who.get("email") or who.get("name") or "authenticated"
    except json.JSONDecodeError:
        owner = first_line(output)

    code, services_output = run(["render", "services", "-o", "json"], timeout=40)
    if code != 0:
        if "no workspace set" in services_output.lower():
            return Result("render", "warn", f"{owner}; no active workspace in the CLI",
                          "render workspaces   then: render workspace set <workspace-id>", state)
        return Result("render", "warn", f"{owner}; could not list services",
                      first_line(services_output), state)
    try:
        services = json.loads(services_output)
    except json.JSONDecodeError:
        return Result("render", "warn", f"{owner}; unparseable services list",
                      first_line(services_output), state)
    if isinstance(services, dict):
        services = services.get("services") or services.get("items") or []
    # `render services -o json` wraps each entry as {"service": {...}}.
    services = [item.get("service", item) for item in services if isinstance(item, dict)]
    live = {str(item.get("name", "")).strip() for item in services}

    if not ctx.render_service:
        return Result("render", "ok", f"{owner}; {len(live)} services", env=state)
    if ctx.render_service not in live:
        return Result("render", "fail",
                      f"{owner}; render.yaml names '{ctx.render_service}', which does not exist "
                      f"(live: {', '.join(sorted(n for n in live if n)) or 'none'})",
                      "rename the service in render.yaml to the live one, or vice versa", state)
    matched = next((item for item in services if item.get("name") == ctx.render_service), {})
    detail = f"{owner}; render.yaml service '{ctx.render_service}' exists"
    if matched.get("id"):
        detail += f" ({matched['id']})"
    if str(matched.get("autoDeploy", "")).lower() in ("yes", "true"):
        detail += "; autoDeploy on, so pushes deploy"
    elif state["RENDER_DEPLOY_HOOK_URL"] == "missing":
        detail += "; RENDER_DEPLOY_HOOK_URL unset, so CI would not trigger a deploy"
    return Result("render", "ok", detail, env=state)


# --------------------------------------------------------------------------- sentry


def check_sentry(ctx: Context) -> Result:
    names = ("SENTRY_AUTH_TOKEN", "SENTRY_DSN", "SENTRY_ORG", "SENTRY_PROJECT")
    state = env_state(names)
    if all(value == "missing" for value in state.values()):
        return Result("sentry", "skip", "not configured",
                      "create a Sentry project, then set SENTRY_DSN on the backend and "
                      "SENTRY_AUTH_TOKEN (+ SENTRY_ORG, SENTRY_PROJECT) for the CLI", state)

    detail = []
    status = "ok"

    if state["SENTRY_DSN"] == "set":
        dsn = os.environ.get("SENTRY_DSN", "")
        match = re.match(r"^https?://[^@]+@([^/]+)/(\d+)$", dsn)
        if not match:
            return Result("sentry", "fail", "SENTRY_DSN is malformed",
                          "expect https://<public-key>@<host>/<project-id>", state)
        host, project_id = match.group(1), match.group(2)
        code, _ = http(f"https://{host}/api/{project_id}/store/", timeout=15)
        if code is None:
            return Result("sentry", "fail", f"SENTRY_DSN host {host} is unreachable",
                          "verify the DSN region host", state)
        detail.append(f"ingest DSN reachable ({host} project {project_id})")
    else:
        detail.append("SENTRY_DSN unset: backend errors are not reported")

    if state["SENTRY_AUTH_TOKEN"] == "set":
        if not shutil.which("sentry-cli"):
            return Result("sentry", "warn", "; ".join(detail) + "; sentry-cli not installed",
                          "bash tools/clis/install.sh   (or ONLY=sentry ...)", state)
        code, output = run(["sentry-cli", "info", "--no-defaults"])
        if code != 0:
            return Result("sentry", "fail", first_line(output) or "sentry-cli auth failed",
                          "check SENTRY_AUTH_TOKEN scopes (project:read, release:write)", state)
        detail.append("sentry-cli authenticated")
        org, project = os.environ.get("SENTRY_ORG"), os.environ.get("SENTRY_PROJECT")
        if org and project:
            code, output = run(["sentry-cli", "projects", "list", "--org", org])
            if code == 0 and project not in output:
                status = "warn"
                detail.append(f"SENTRY_PROJECT '{project}' not visible in org '{org}'")
    return Result("sentry", status, "; ".join(detail), env=state)


# --------------------------------------------------------------------------- vercel


def check_vercel(ctx: Context) -> Result:
    state = env_state(("VERCEL_TOKEN", "VERCEL_ORG_ID", "VERCEL_PROJECT_ID"))
    if state["VERCEL_TOKEN"] == "missing":
        return Result("vercel", "skip", "VERCEL_TOKEN not set (no CLI session)",
                      "vercel.com/account/tokens, then export VERCEL_TOKEN", state)
    if not shutil.which("vercel"):
        return Result("vercel", "fail", "vercel CLI not installed",
                      "bash tools/clis/install.sh   (or ONLY=vercel ...)", state)
    code, output = run(["vercel", "whoami"])
    if code != 0:
        return Result("vercel", "fail", first_line(output) or "whoami failed",
                      "check the VERCEL_TOKEN value", state)
    return Result("vercel", "ok", f"authenticated as {first_line(output, 60)}", env=state)


# --------------------------------------------------------------------------- web


def app_package_id() -> str:
    path = REPO_ROOT / "app" / "build.gradle.kts"
    if not path.is_file():
        return ""
    match = re.search(r'applicationId\s*=\s*"([^"]+)"', path.read_text())
    return match.group(1) if match else ""


def check_web(ctx: Context) -> Result:
    if ctx.offline:
        return Result("web", "skip", "offline mode")
    code, body = http(ctx.web_url, timeout=25)
    if code is None:
        return Result("web", "fail", f"unreachable: {body[:120]}", f"check the deployment at {ctx.web_url}")
    if code >= 400:
        return Result("web", "fail", f"HTTP {code}", "check the Vercel deployment and domain")
    marker = "Wurie" if "Wurie" in body else ""
    detail = f"HTTP {code}" + (f"; '{marker}' present" if marker else "; page marker not found")
    return Result("web", "ok" if marker else "warn", detail, "confirm the deployed site is the current one")


# --------------------------------------------------------------------------- firebase


def check_firebase(ctx: Context) -> Result:
    state = env_state(("FIREBASE_PROJECT_ID", "GOOGLE_APPLICATION_CREDENTIALS", "WEB_CLIENT_ID"))
    config_path = REPO_ROOT / "app" / "google-services.json"
    if not config_path.is_file():
        return Result("firebase", "fail", "app/google-services.json is missing",
                      "download it from Firebase project settings and commit it", state)
    try:
        config = json.loads(config_path.read_text())
    except json.JSONDecodeError as error:
        return Result("firebase", "fail", f"google-services.json is not valid JSON: {error}",
                      "re-download the config", state)

    project_id = config.get("project_info", {}).get("project_id", "")
    clients = config.get("client", []) or []
    client = clients[0] if clients else {}
    package_name = client.get("client_info", {}).get("android_client_info", {}).get("package_name", "")
    app_id = app_package_id()
    has_api_key = bool((client.get("api_key") or [{}])[0].get("current_key"))
    oauth_types = [str(item.get("client_type")) for item in client.get("oauth_client", []) or []]

    if package_name and app_id and package_name != app_id:
        return Result("firebase", "fail",
                      f"google-services.json targets '{package_name}' but the app id is '{app_id}'",
                      "re-download google-services.json for the current applicationId", state)
    if not has_api_key:
        return Result("firebase", "fail", "google-services.json has no API key",
                      "re-download the config", state)

    # Firestore rules are security configuration: the client writes to Firestore
    # directly, so unversioned or permissive rules are a real exposure.
    rules_path = REPO_ROOT / "firestore.rules"
    if not rules_path.is_file():
        return Result("firebase", "fail", "firestore.rules is not committed",
                      "commit deny-by-default rules and deploy them with: firebase deploy --only firestore:rules",
                      state)
    rules = rules_path.read_text()
    permissive = [
        line.strip() for line in rules.splitlines()
        if "allow" in line and re.search(r"if\s+true\s*;?", line) and "if false" not in line
    ]
    if permissive:
        return Result("firebase", "fail",
                      f"firestore.rules has permissive rules: {permissive[0][:80]}",
                      "replace with owner-scoped conditions (see firestore.rules)", state)
    detail = [f"project '{project_id}', package '{package_name}' matches"]
    status = "ok"
    if re.search(r"allow\s+read,\s*write:\s*if\s+false", rules):
        detail.append("firestore.rules deny-by-default")
    else:
        status = "warn"
        detail.append("firestore.rules has no deny-all catch-all for future collections")
    if "3" not in oauth_types:
        status = "warn"
        detail.append("no web OAuth client in the config: WEB_CLIENT_ID cannot be valid yet")
    if state["FIREBASE_PROJECT_ID"] == "set" and project_id and os.environ["FIREBASE_PROJECT_ID"] != project_id:
        status = "warn"
        detail.append("backend FIREBASE_PROJECT_ID differs from the app project")
    if state["GOOGLE_APPLICATION_CREDENTIALS"] == "missing":
        detail.append("GOOGLE_APPLICATION_CREDENTIALS unset (Admin SDK uses the app default)")

    if not ctx.offline and shutil.which("firebase") and (
        state["GOOGLE_APPLICATION_CREDENTIALS"] == "set" or os.environ.get("FIREBASE_TOKEN")
    ):
        code, output = run(["firebase", "projects:list", "--json"], timeout=60)
        if code == 0:
            detail.append("firebase CLI authenticated")
        else:
            status = "warn"
            detail.append("firebase CLI could not list projects: " + first_line(output, 80))
    return Result("firebase", status, "; ".join(detail), env=state)


# --------------------------------------------------------------------------- google ai


def check_google_ai(ctx: Context) -> Result:
    state = env_state(("GEMINI_API_KEY", "GOOGLE_API_KEY", "GEMINI_MODEL"))
    key = os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
    if not key:
        return Result("google_ai", "skip", "no Gemini key: the agent runtime stays degraded",
                      "aistudio.google.com/apikey, then set GEMINI_API_KEY in the backend environment",
                      state)
    if ctx.offline:
        return Result("google_ai", "skip", "offline mode", env=state)

    code, body = http(GEMINI_MODELS_URL, headers={"x-goog-api-key": key})
    if code is None:
        return Result("google_ai", "fail", f"unreachable: {body[:120]}",
                      "check network egress from the backend", state)
    if code != 200:
        return Result("google_ai", "fail", f"API rejected the key (HTTP {code})",
                      "rotate the key or enable the Generative Language API", state)
    try:
        models = [item.get("name", "").split("/")[-1] for item in json.loads(body).get("models", [])]
    except json.JSONDecodeError:
        models = []
    detail = f"key valid; {len(models)} models available"
    model = os.environ.get("GEMINI_MODEL", "")
    if model and models and model not in models:
        return Result("google_ai", "warn", detail + f"; GEMINI_MODEL '{model}' is not in the list",
                      "pick a model from the list or leave GEMINI_MODEL unset", state)
    return Result("google_ai", "ok", detail + (f"; {model} available" if model else ""), env=state)


# --------------------------------------------------------------------------- langsmith


def check_langsmith(ctx: Context) -> Result:
    state = env_state(("LANGSMITH_API_KEY", "LANGSMITH_PROJECT", "LANGSMITH_ENDPOINT"))
    key = os.environ.get("LANGSMITH_API_KEY")
    if not key:
        return Result("langsmith", "skip", "no LangSmith key: agent traces are not uploaded",
                      "smith.langchain.com -> Settings -> API keys, then set LANGSMITH_API_KEY", state)
    if ctx.offline:
        return Result("langsmith", "skip", "offline mode", env=state)
    endpoint = os.environ.get("LANGSMITH_ENDPOINT", "https://api.smith.langchain.com").rstrip("/")
    code, body = http(f"{endpoint}/api/v1/workspaces", headers={"x-api-key": key})
    if code is None:
        return Result("langsmith", "fail", f"unreachable: {body[:120]}", "check LANGSMITH_ENDPOINT", state)
    if code in (401, 403):
        return Result("langsmith", "fail", f"key rejected (HTTP {code})", "regenerate the API key", state)
    if code != 200:
        return Result("langsmith", "warn", f"HTTP {code} from {endpoint}",
                      "confirm the endpoint and key permissions", state)
    project = os.environ.get("LANGSMITH_PROJECT", "")
    return Result("langsmith", "ok", "authenticated" + (f"; project '{project}'" if project else ""),
                  env=state)


# --------------------------------------------------------------------------- backend


def check_backend(ctx: Context) -> Result:
    if ctx.offline:
        return Result("backend", "skip", "offline mode")
    base = ctx.backend_url.rstrip("/")
    code, body = http(f"{base}/health", timeout=30)
    if code is None:
        return Result("backend", "fail", f"unreachable: {body[:120]}",
                      "check the Render service and its deploy status")
    if code != 200:
        return Result("backend", "fail", f"HTTP {code}", "read the deploy logs on Render")
    try:
        payload = json.loads(body)
    except json.JSONDecodeError:
        return Result("backend", "fail", "/health did not return JSON", "redeploy the backend")
    missing = [key for key in HEALTH_CONTRACT if key not in payload]
    summary = " ".join(
        f"{key}={payload[key]}"
        for key in ("agent_available", "firebase_admin", "storage", "auth_mode", "app_check")
        if key in payload
    )
    if missing:
        return Result("backend", "warn", f"HTTP 200 but contract fields missing: {', '.join(missing)}",
                      "the running revision is behind the code: redeploy the backend")
    if str(payload.get("agent_available", "")).lower() != "true":
        return Result("backend", "warn", f"HTTP 200; {summary}",
                      "the agent runtime is degraded: " + str(payload.get("agent_reason", ""))[:120])
    return Result("backend", "ok", summary)


CHECKS = {
    "github": check_github,
    "ci": check_ci,
    "render": check_render,
    "sentry": check_sentry,
    "vercel": check_vercel,
    "web": check_web,
    "firebase": check_firebase,
    "google_ai": check_google_ai,
    "langsmith": check_langsmith,
    "backend": check_backend,
}

COLORS = {"ok": "\033[1;32m", "warn": "\033[1;33m", "fail": "\033[1;31m", "skip": "\033[1;90m"}


def load_env_files() -> list:
    """Load .env / .env.local the way the app does. Values are never printed."""
    loaded = []
    for candidate in (REPO_ROOT / ".env", REPO_ROOT / ".env.local",
                      REPO_ROOT / "wurie-backend" / ".env"):
        if not candidate.is_file():
            continue
        try:
            for line in candidate.read_text().splitlines():
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                key = key.strip().removeprefix("export ").strip()
                value = value.strip().strip('"').strip("'")
                if key and value and not os.environ.get(key):
                    os.environ[key] = value
        except OSError:
            continue
        loaded.append(str(candidate.relative_to(REPO_ROOT)))
    return loaded


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--only", help="comma-separated services to check")
    parser.add_argument("--json", action="store_true", help="machine-readable output")
    parser.add_argument("--offline", action="store_true", help="skip every network probe")
    parser.add_argument("--strict", action="store_true", help="treat skips as failures")
    parser.add_argument("--no-env", action="store_true", help="do not load .env files")
    parser.add_argument("--gh-evidence", default=os.environ.get("WURIE_GH_EVIDENCE", ""),
                        help="directory of gh probe output collected by tools/check-services.sh")
    parser.add_argument("--backend-url", default=os.environ.get("WURIE_API_BASE_URL", DEFAULT_BACKEND_URL))
    parser.add_argument("--web-url", default=os.environ.get("WURIE_WEB_URL", DEFAULT_WEB_URL))
    args = parser.parse_args(argv)

    loaded_env = [] if args.no_env else load_env_files()
    ctx = Context(offline=args.offline, backend_url=args.backend_url, web_url=args.web_url,
                  gh_dir=args.gh_evidence)

    selected = list(CHECKS)
    if args.only:
        selected = [name.strip() for name in args.only.split(",") if name.strip()]
        unknown = [name for name in selected if name not in CHECKS]
        if unknown:
            parser.error(f"unknown service(s): {', '.join(unknown)} (known: {', '.join(CHECKS)})")

    results = [CHECKS[name](ctx) for name in selected]
    failures = [r for r in results if r.status == "fail"]
    skips = [r for r in results if r.status == "skip"]
    warns = [r for r in results if r.status == "warn"]
    exit_code = 1 if failures or (args.strict and skips) else 0

    if args.json:
        print(json.dumps({
            "services": [asdict(r) for r in results],
            "env_files": loaded_env,
            "summary": {
                "ok": sum(1 for r in results if r.status == "ok"),
                "warn": len(warns), "fail": len(failures), "skip": len(skips),
                "exit_code": exit_code,
            },
        }, indent=2))
        return exit_code

    color = sys.stdout.isatty()
    print(f"WurieAI service connections{'  (offline)' if ctx.offline else ''}")
    if loaded_env:
        print(f"env files loaded: {', '.join(loaded_env)} (names only, values never printed)")
    print()
    width = max(len(r.service) for r in results)
    for result in sorted(results, key=lambda r: (STATUS_ORDER[r.status], r.service)):
        label = result.status.upper()
        if color:
            label = f"{COLORS[result.status]}{label}\033[0m"
        print(f"  {result.service.ljust(width)}  {label.ljust(12 if not color else 18)}  {result.detail}")
        if result.fix and result.status in ("fail", "skip", "warn"):
            print(f"  {' ' * width}  {'':<12}  -> {result.fix}")
        env_line = ", ".join(f"{name}={state}" for name, state in result.env.items())
        if env_line and result.status in ("fail", "skip", "warn"):
            print(f"  {' ' * width}  {'':<12}  env: {env_line}")

    print()
    print(f"  {len(results)} checked: {sum(1 for r in results if r.status == 'ok')} ok, "
          f"{len(warns)} warn, {len(failures)} fail, {len(skips)} skip")
    if failures:
        print("  blocking: " + ", ".join(r.service for r in failures))
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
