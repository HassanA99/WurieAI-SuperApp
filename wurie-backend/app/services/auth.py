from __future__ import annotations

import os
import json
import logging
import tempfile

import firebase_admin
from firebase_admin import app_check as firebase_app_check
from firebase_admin import auth as firebase_auth
from firebase_admin import credentials
from fastapi import Header, HTTPException

logger = logging.getLogger("wurieai.auth")

# Development-only escape hatch. It is refused outright on any runtime that identifies
# itself as production (see _dev_fallback_enabled), so a misconfigured production
# environment cannot silently disable authentication.
_DEV_FLAG = "WURIE_DEV_AUTH_FALLBACK"
_TRUTHY = {"1", "true", "yes", "on"}
# Runtimes that always mean production. Render sets RENDER=true for every service.
_PRODUCTION_MARKERS = ("RENDER", "K_SERVICE", "WEBSITE_INSTANCE_ID")


def _truthy(value: str | None) -> bool:
    return bool(value) and value.strip().lower() in _TRUTHY


def environment_name() -> str:
    return (
        os.getenv("SENTRY_ENVIRONMENT")
        or os.getenv("WURIE_ENV")
        or ("production" if any(os.getenv(marker) for marker in _PRODUCTION_MARKERS) else "development")
    )


def is_production() -> bool:
    """True when the runtime identifies itself as production."""
    if os.getenv("WURIE_ENV", "").strip().lower() == "production":
        return True
    if os.getenv("SENTRY_ENVIRONMENT", "").strip().lower() == "production":
        return True
    return any(os.getenv(marker) for marker in _PRODUCTION_MARKERS)


def dev_fallback_requested() -> bool:
    """Whether the dev flag is set at all, before the production guard."""
    return _truthy(os.getenv(_DEV_FLAG))


def _dev_fallback_enabled() -> bool:
    """Dev auth fallback, refused in production.

    Returns True only when the operator explicitly opted in *and* the runtime is not
    production. A production runtime that sets the flag is reported loudly instead of
    quietly accepting unauthenticated requests.
    """
    if not dev_fallback_requested():
        return False
    if is_production():
        logger.error(
            "%s is set in a production runtime (%s): refusing to disable authentication",
            _DEV_FLAG,
            environment_name(),
        )
        return False
    return True


def app_check_required() -> bool:
    """Whether App Check tokens must accompany authenticated requests."""
    return _truthy(os.getenv("WURIE_APP_CHECK_ENFORCE"))


def firebase_admin_ready() -> bool:
    """Return True when Firebase Admin has an initialised app.

    Used for external readiness reporting: when this is False the backend still runs,
    but ID-token verification is unavailable and writes fall back to memory.
    """
    return bool(firebase_admin._apps)


def initialize_firebase_admin() -> None:
    """Initialize Firebase Admin using explicit project credentials when available.

    Supported sources, in order:
    1. FIREBASE_SERVICE_ACCOUNT_JSON
    2. FIREBASE_SERVICE_ACCOUNT_PATH
    3. GOOGLE_APPLICATION_CREDENTIALS
    4. default application credentials on GCP/Cloud Run

    This keeps the backend ready for real Firebase auth while still allowing local
    development to opt into the explicit dev fallback flag.
    """
    if firebase_admin._apps:
        return

    service_account_json = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")
    if service_account_json:
        try:
            payload = json.loads(service_account_json)
            with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as tmp:
                json.dump(payload, tmp)
                tmp.flush()
                firebase_admin.initialize_app(credentials.Certificate(tmp.name))
            return
        except Exception:
            raise RuntimeError("FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON")

    service_account_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
    if service_account_path:
        firebase_admin.initialize_app(credentials.Certificate(service_account_path))
        return

    if os.getenv("GOOGLE_APPLICATION_CREDENTIALS"):
        firebase_admin.initialize_app()
        return

    try:
        firebase_admin.initialize_app()
    except Exception:
        if _dev_fallback_enabled():
            return
        raise


def _verify_app_check(app_check_token: str | None) -> None:
    """Enforce Firebase App Check when configured to do so.

    App Check answers "is this request coming from our genuine app build", which is what
    stops a leaked or scripted backend call from being indistinguishable from a real
    client. Enforcement is opt-in, because it cannot be turned on before the Android and
    web builds ship the provider and the Firebase console has registered them.
    """
    if not app_check_required():
        return

    if not app_check_token:
        raise HTTPException(status_code=401, detail="Missing App Check token")

    try:
        initialize_firebase_admin()
        firebase_app_check.verify_token(app_check_token)
    except HTTPException:
        raise
    except Exception as exc:
        # Not exc_info: an invalid App Check token is an expected client-side condition
        # (stale or missing provider), and the traceback would drown the signal.
        logger.warning("App Check token rejected: %s", exc)
        raise HTTPException(status_code=401, detail="Invalid App Check token")


async def verify_firebase_token(
    authorization: str | None = Header(default=None),
    x_firebase_appcheck: str | None = Header(default=None),
) -> dict:
    """Authenticate a request and return the caller identity.

    Policy, in order:

    1. App Check is verified first when enforcement is enabled.
    2. A missing or malformed credential is rejected. No token value is ever trusted
       without cryptographic verification - there is deliberately no magic string that
       grants an identity, because such a string becomes an authentication bypass the
       moment the code reaches production.
    3. The only identity granted without a valid ID token is `dev_user`, and only when
       the explicit development flag is set on a non-production runtime.
    """
    _verify_app_check(x_firebase_appcheck)
    dev_fallback = _dev_fallback_enabled()

    if not authorization:
        if dev_fallback:
            return {"uid": "dev_user", "claims": {}, "dev": True}
        raise HTTPException(status_code=401, detail="Missing authorization header")

    if not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization scheme")

    token = authorization.split(" ", 1)[1].strip()
    if not token:
        raise HTTPException(status_code=401, detail="Missing bearer token")

    try:
        initialize_firebase_admin()

        decoded = firebase_auth.verify_id_token(token)
        uid = decoded.get("uid")
        if not uid:
            raise HTTPException(status_code=401, detail="Firebase token did not contain a uid")

        return {"uid": uid, "claims": decoded, "dev": False}
    except HTTPException:
        raise
    except Exception:
        if dev_fallback:
            logger.warning("ID token verification failed; using the development fallback identity")
            return {"uid": "dev_user", "claims": {}, "dev": True}

        logger.info("Rejected request with an unverifiable ID token")
        raise HTTPException(status_code=401, detail="Invalid authentication credentials")


def auth_health() -> dict:
    """Auth posture for /health and /api/v1/runtime/status."""
    if dev_fallback_requested() and not _dev_fallback_enabled():
        mode = "refused_dev_fallback"
    elif _dev_fallback_enabled():
        mode = "dev_fallback"
    else:
        mode = "firebase"

    return {
        "mode": mode,
        "environment": environment_name(),
        "app_check": "enforced" if app_check_required() else "off",
    }
