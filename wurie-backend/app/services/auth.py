from __future__ import annotations

import os
import json
import tempfile

import firebase_admin
from firebase_admin import auth as firebase_auth
from firebase_admin import credentials
from fastapi import Header, HTTPException


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
        if os.getenv("WURIE_DEV_AUTH_FALLBACK", "0").lower() in {"1", "true", "yes"}:
            return
        raise


async def verify_firebase_token(authorization: str | None = Header(default=None)) -> dict:
    """Authenticate an incoming request using a Firebase ID token.

    The app should reject missing credentials in production. Local-only
    fallback can be enabled explicitly through an environment flag.
    """

    # Fail closed: missing authentication header is never a valid user.
    if not authorization:
        if os.getenv("WURIE_DEV_AUTH_FALLBACK", "0").lower() in {"1", "true", "yes"}:
            return {"uid": "dev_user"}
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

        return {"uid": uid, "claims": decoded}
    except HTTPException:
        raise
    except Exception:
        # Explicit local fallback is still allowed only when the developer intentionally enables it.
        if os.getenv("WURIE_DEV_AUTH_FALLBACK", "0").lower() in {"1", "true", "yes"}:
            return {"uid": "dev_user"}

        raise HTTPException(status_code=401, detail="Invalid authentication credentials")
