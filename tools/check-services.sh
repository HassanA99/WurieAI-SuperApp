#!/usr/bin/env bash
#
# Verify every external service WurieAI depends on - one command, no secrets printed.
#
# This is the recommended entry point. It exists because Freebuff attaches its
# repository-scoped GitHub credential to direct `gh` calls, so the GitHub probes have to
# run here in the shell; the collected JSON is then handed to tools/check-services.py,
# which probes everything else (Render, Sentry, Vercel, Firebase, Gemini, LangSmith, the
# live backend and the web app).
#
# Usage:
#   bash tools/check-services.sh
#   bash tools/check-services.sh --only render,backend --json
#   bash tools/check-services.sh --strict          # skips count as failures (CI gate)
#   bash tools/check-services.sh --offline         # repo/config checks only
#
# Credentials come from the environment (and from .env / .env.local, which this script
# loads through check-services.py - values are never printed).
#
# NOTE for Freebuff Cloud workspaces: the managed GitHub credential is attached to a
# command when that command itself invokes gh. If the GitHub checks report "skipped",
# run them in the same command as the wrapper:
#
#   gh --version >/dev/null; bash tools/check-services.sh
#
# On a normal developer machine a one-time `gh auth login` is enough.
#
# Exit codes: 0 = every configured service connected, 1 = at least one failure.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE_DIR="$(mktemp -d)"
trap 'rm -rf "$EVIDENCE_DIR"' EXIT

log() { printf '\033[1;34m==>\033[0m %s\n' "$*" >&2; }

if ! command -v gh >/dev/null 2>&1; then
  log "gh not installed; GitHub checks will be skipped (bash tools/clis/install.sh)"
else
  log "collecting GitHub evidence (repo, workflow runs, failing steps)"
  gh repo view --json nameWithOwner,defaultBranchRef \
    >"$EVIDENCE_DIR/repo.json" 2>"$EVIDENCE_DIR/repo.err" || true

  gh run list --limit 40 --json workflowName,status,conclusion,headBranch,createdAt,databaseId \
    >"$EVIDENCE_DIR/runs.json" 2>"$EVIDENCE_DIR/runs.err" || true

  # Actions secrets are not readable with an app installation token (HTTP 403); that is
  # expected and is recorded rather than treated as a failure.
  gh secret list >"$EVIDENCE_DIR/secrets.txt" 2>"$EVIDENCE_DIR/secrets.err" || true

  # Failing steps for the most recent failing runs, so the report names the step.
  failing_ids="$(gh run list --status failure --limit 3 --json databaseId --jq '.[].databaseId' 2>/dev/null || true)"
  for run_id in $failing_ids; do
    gh run view "$run_id" --json jobs \
      --jq '[.jobs[] | select(.conclusion == "failure") | .name + " / " + ([.steps[]? | select(.conclusion == "failure") | .name] | join(", "))] | join("; ")' \
      >"$EVIDENCE_DIR/run-$run_id.txt" 2>/dev/null || true
  done
fi

exec python3 "$REPO_ROOT/tools/check-services.py" --gh-evidence "$EVIDENCE_DIR" "$@"
