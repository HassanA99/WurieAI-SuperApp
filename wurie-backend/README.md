# WurieAI Backend

The backend targets Python 3.10 through 3.12. The Docker image uses Python 3.10, but local development is now managed with uv and Python 3.12 is the recommended interpreter for compatibility with the pinned dependencies.

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
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

The health check is available at `http://localhost:8080/health`.

## Test

```bash
uv run python -m unittest app.test_domain_router
```

## Notes

- uv replaces the old pip-based local environment flow.
- Dependencies are defined in `pyproject.toml` and resolved by uv.
- `requirements.txt` remains as a compatibility fallback for other tools and container workflows.