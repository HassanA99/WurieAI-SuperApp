from __future__ import annotations

import os

import firebase_admin
from firebase_admin import auth as firebase_auth
from fastapi import Header, HTTPException


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
        # Prefer the real Firebase backend verifier when Firebase admin is initialized.
        if not firebase_admin._apps:
            raise HTTPException(status_code=401, detail="Firebase Admin is not initialized")

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
