# WurieAI Backend

The backend targets Python 3.11 through 3.13. The Docker image and local development use Python 3.12.

The dependency stack (FastAPI, LangChain core, langchain-google-genai, LangGraph, LangSmith, pydantic) is kept on current releases: the previous pins failed to import on Python 3.12, which silently disabled the whole agent path.

## Local setup

```bash
cd wurie-backend
uv python install 3.12
uv sync --python 3.12
cp .env.example .env
```

Set Firebase credentials in `.env` before starting authenticated routes. Keep `WURIE_DEV_AUTH_FALLBACK=false` outside local-only development.

## Run

```bash
uv run --env-file .env uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

The health check is available at `http://localhost:8080/health`.

## Agent runtime

The assistant prefers the LangGraph agent runtime and reports honestly when it cannot use it:

- `GEMINI_API_KEY` (or `GOOGLE_API_KEY`) enables the agent runtime. Without it, `/health` reports `agent.available = false` with the reason, and chat answers are served by the deterministic domain router with `data.degraded = true` plus `data.degraded_reason`.
- `GEMINI_MODEL` selects the model (default `gemini-2.5-flash`).
- `WURIE_AGENT_FALLBACK` controls degradation: `router` (default) serves the deterministic answer, `error` returns HTTP 503 instead.
- Agent runs are logged with a `workflow_id` that is also returned to the client.

## Authentication and client trust

Every authenticated endpoint resolves identity through one policy in `app/services/auth.py`:

- **No token value is trusted without verification.** There is no `guest-token` or `dev_user` string that grants an identity; previously such a string was accepted before any verification, which would have handed every anonymous caller the same authenticated user on the next deploy.
- **A missing or malformed credential is `401`.** App Check is checked first when enforcement is on, then the Azure-style bearer token is verified with Firebase Admin.
- **The development fallback is local-only.** `WURIE_DEV_AUTH_FALLBACK=true` grants `dev_user` only on a runtime that is *not* production. On Render, Cloud Run (`K_SERVICE`), or with `SENTRY_ENVIRONMENT=production` the flag is ignored, logged as an error, and reported as `auth_mode=refused_dev_fallback` on `/health`.
- **App Check enforcement** (`WURIE_APP_CHECK_ENFORCE=true`) requires a valid `X-Firebase-AppCheck` token and fails closed. Enable it only after the clients ship a registered App Check provider.

Firestore rules are versioned in `firestore.rules` (deny-by-default; a signed-in user may only reach their own `users/{uid}` document and its subcollections, everything else is server-only through the Admin SDK). Deploy them with:

```bash
firebase deploy --only firestore:rules
```

CI fails the build if `firestore.rules` is missing or contains a permissive (`if true`) rule, because the Android client writes to Firestore directly. The Android client no longer sends a placeholder credential: signed-out calls are rejected instead of being attributed to a shared guest user.

Environment variables: `WURIE_DEV_AUTH_FALLBACK=false`, `WURIE_APP_CHECK_ENFORCE=false` (both expected in production). `wurie-backend/.env.example` lists the rest.

## Deployment verification

`GET /health` always returns 200 and reports runtime readiness as flat strings, so a broken
AI path, missing Firebase credentials, or in-memory storage is visible from outside without
failing the platform health check:

| Field | Meaning |
| --- | --- |
| `status` | liveness (`healthy`) |
| `agent_available` / `agent_model` / `agent_fallback` / `agent_reason` | whether the agent runtime can answer, and why not |
| `firebase_admin` | whether Firebase Admin initialized (ID tokens work) |
| `storage` | `firestore` when writes are durable, `memory` when they are per-process |
| `error_tracking` | `sentry` when error reporting is enabled, `off` otherwise |
| `auth_mode` | `firebase` (verified ID tokens), `dev_fallback` (local only), or `refused_dev_fallback` (the flag was set on a production runtime and ignored) |
| `app_check` | `enforced` when App Check tokens are required, `off` otherwise |

Structured detail (including the fallback mode) is available on the authenticated
`GET /api/v1/runtime/status`.

Check whether the running deployment matches the code (routes and health contract):

```bash
uv run python -m app.deployment_check https://wurieai-superapp.onrender.com
```

Exit codes: `0` in sync, `1` drift detected, `2` unreachable. The same check runs automatically
in the `verify` job of `.github/workflows/deploy-backend.yml` after every backend deploy, so a
stale revision fails the workflow instead of serving old code silently.

## Error tracking

Set `SENTRY_DSN` to enable server error reporting (optional: `SENTRY_ENVIRONMENT`,
`SENTRY_RELEASE`, `SENTRY_TRACES_SAMPLE_RATE`). Without a DSN the backend boots normally and
logs a warning; `/health` reports `error_tracking = off`.

## Test

```bash
uv run python -m unittest app.test_domain_router app.test_agent_runtime app.test_deployment_check
```

`app.test_agent_runtime` is the regression gate for the agent path: it fails the build if the orchestrator stops importing, if the graph loses a node, or if chat degradation ever becomes silent again.

## Notes

- uv replaces the old pip-based local environment flow.
- Dependencies are defined in `pyproject.toml` and resolved by uv.
- `requirements.txt` remains as a compatibility fallback for other tools and container workflows, pinned to the same versions resolved in `uv.lock`.
- Render supplies production environment variables from its dashboard; GitHub Actions only triggers the Render deploy hook.