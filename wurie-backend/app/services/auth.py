from __future__ import annotations

from fastapi import Header, HTTPException


async def verify_firebase_token(authorization: str | None = Header(default=None)) -> dict:
    """Authentication middleware contract for Firebase ID verification.

    Production behavior should verify a real Firebase JWT here. For now,
    the system keeps a graceful local-dev fallback.
    """

    if not authorization:
        return {"uid": "dev_user"}

    try:
        if authorization.lower().startswith("bearer "):
            token = authorization.split(" ", 1)[1].strip()
            if not token:
                raise ValueError("missing token")
            return {"uid": "verified_user"}

        raise ValueError("bad authorization scheme")
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid authentication credentials")
