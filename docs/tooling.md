# WurieAI tooling and service connections

One CLI per service, one command to verify every connection. No credential value is ever
printed by these tools - only variable names and yes/no states.

## Install the toolchain

```bash
bash tools/clis/install.sh                 # everything
ONLY=render,sentry bash tools/clis/install.sh
CLI_INSTALL_DIR="$HOME/.local/bin" bash tools/clis/install.sh
```

| Service | CLI | Used for |
| --- | --- | --- |
| GitHub | `gh` | repositories, Actions runs and logs, releases |
| Render | `render` | backend deploys, logs, service inventory, env vars |
| Firebase | `firebase` | Auth, Firestore rules/indexes, emulators |
| Vercel | `vercel` | web app (`wurieai-web.vercel.app`) deploys |
| Sentry | `sentry-cli` | releases, source maps, error lookups |
| LangSmith | *none* | verified over its HTTP API by the checker |

Installs are version-pinned, and checksums are verified whenever upstream publishes them.

## Verify every connection

```bash
bash tools/check-services.sh                 # human report
bash tools/check-services.sh --json          # machine readable
bash tools/check-services.sh --only render,backend
bash tools/check-services.sh --strict        # a skip counts as a failure (CI gate)
bash tools/check-services.sh --offline       # repo/config checks only, no network
```

Statuses: `ok` verified, `warn` connected but something in the repo/config does not line
up, `fail` credentials are present but unusable, `skip` nothing to connect yet (the report
says how to get the credential). Exit code `1` means at least one `fail`.

The shell wrapper exists for one reason: Freebuff Cloud attaches its repository-scoped
GitHub credential to a command that invokes `gh` itself, so the wrapper runs the `gh`
probes and passes the collected JSON to `tools/check-services.py`, which probes everything
else. If the GitHub checks report `skip`, run them together with a `gh` call:

```bash
gh --version >/dev/null; bash tools/check-services.sh
```

On a developer machine a one-time `gh auth login` is enough.

What is checked, per service:

- **github** - repo readable and default branch; workflow-referenced secrets that the
  repository does not define (unreadable with an app credential, reported as such).
- **ci** - latest Actions run per workflow, with the exact failing step.
- **render** - API key valid, and the `render.yaml` service name exists in the account.
- **sentry** - DSN reachable (ingest) and `sentry-cli` authenticated (management).
- **vercel** - token valid.
- **web** - `wurieai-web.vercel.app` responds and serves the app.
- **firebase** - `google-services.json` package matches `applicationId`, API key present,
  OAuth client present, project id agrees with the backend.
- **google_ai** - Gemini key valid, and `GEMINI_MODEL` exists in the model list.
- **langsmith** - API key valid against the configured endpoint.
- **backend** - live `/health` answers **and** exposes the full flat contract.

## Credentials

Secrets are never committed and never read by these tools. Set the values in the place
below and the checker picks them up from the environment.

| Variable | Where it belongs | Enables |
| --- | --- | --- |
| `RENDER_API_KEY` | local env / `RENDER_API_KEY` GitHub secret | Render CLI (deploys, logs, service inventory) |
| `RENDER_DEPLOY_HOOK_URL` | GitHub repository secret | optional explicit deploy trigger (see below) |
| `SENTRY_DSN` | Render dashboard env var | backend error reporting (`/health` -> `error_tracking=sentry`) |
| `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT` | local env / GitHub secrets | `sentry-cli` releases and lookups |
| `VERCEL_TOKEN` | local env | Vercel CLI |
| `GEMINI_API_KEY` | Render dashboard env var | the agent runtime (`/health` -> `agent_available`) |
| `LANGSMITH_API_KEY` | Render dashboard env var | agent trace upload |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Render dashboard env var | ID tokens + Firestore (`/health` -> `storage=firestore`) |
| `WEB_CLIENT_ID` | local `local.properties` / env | Google Sign-In on Android |

## Deployment facts (verified through the Render CLI)

```bash
render services -o json                     # service inventory
render deploys list srv-dajfmr7qj5pc73detu40
render logs --service srv-dajfmr7qj5pc73detu40
render deploys create srv-dajfmr7qj5pc73detu40 --confirm --wait
```

- Service **`WurieAI-SuperApp`** (`srv-dajfmr7qj5pc73detu40`), Docker runtime,
  `wurie-backend/Dockerfile`, branch `main`, health check `/health`, plan `free`, region
  `oregon`, public URL `https://wurieai-superapp.onrender.com`.
- The service name is case-sensitive and is **not** the hostname. An earlier revision of
  `render.yaml` named a service that does not exist.
- `autoDeploy` is enabled, so a push to `main` deploys. That is why the workflow only
  triggers a deploy hook when `RENDER_DEPLOY_HOOK_URL` is configured - otherwise every
  push would queue two deploys of the same commit.

## Client trust configuration

Security configuration is versioned and deployed with the Firebase CLI:

```bash
firebase deploy --only firestore:rules     # firestore.rules (deny-by-default, owner-scoped)
```

`tools/check-services.py --offline --only firebase` (run in the CI `verify` job) fails when
`firestore.rules` is missing or contains a permissive `if true` rule, and warns when the
backend has no App Check enforcement. Auth posture is visible on the backend without
credentials: `curl -s <backend>/health | jq '{auth_mode, app_check}'`.

| Switch | Production value | Meaning |
| --- | --- | --- |
| `WURIE_DEV_AUTH_FALLBACK` | `false` | local-only identity; refused on production runtimes |
| `WURIE_APP_CHECK_ENFORCE` | `false` until clients ship App Check | require a valid `X-Firebase-AppCheck` token |

## Deeper checks

```bash
# route + health-contract drift between the deployed revision and the code
cd wurie-backend && uv run python -m app.deployment_check https://wurieai-superapp.onrender.com

# why a workflow failed, with step output
gh run view <run-id> --log-failed

# backend test suite (includes the agent-runtime and deployment-check gates)
cd wurie-backend && uv run python -m unittest app.test_domain_router app.test_agent_runtime app.test_deployment_check
```
