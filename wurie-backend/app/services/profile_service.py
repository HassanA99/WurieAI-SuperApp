from __future__ import annotations

from typing import Any

import firebase_admin
from firebase_admin import firestore

from app.services.service_contracts import ProfileSettings, UserProfile


_profiles: dict[str, dict[str, Any]] = {}
_settings: dict[str, dict[str, Any]] = {}


class ProfileService:
    """Read and update user-owned profile data.

    Firestore is used when Firebase Admin is initialized. The in-memory store is
    deliberately retained for local tests and development without credentials.
    """

    def _firestore_enabled(self) -> bool:
        return bool(firebase_admin._apps)

    def get_profile(self, uid: str, email: str = "") -> UserProfile:
        if self._firestore_enabled():
            data = firestore.client().collection("users").document(uid).get().to_dict() or {}
        else:
            data = _profiles.get(uid, {})

        full_name = data.get("full_name", data.get("fullName", "Wurie Explorer"))
        profile_email = data.get("email", email)
        return UserProfile(
            uid=uid,
            full_name=full_name,
            email=profile_email,
            phone=data.get("phone"),
            city=data.get("city"),
            role=data.get("role", "customer"),
            profile_completed=bool(data.get("profile_completed", False)),
        )

    def update_profile(self, uid: str, updates: dict[str, Any], email: str = "") -> UserProfile:
        current = self.get_profile(uid, email)
        data = current.model_dump()
        data.update({key: value for key, value in updates.items() if value is not None})
        data["profile_completed"] = bool(data.get("full_name") and data.get("phone") and data.get("city"))

        if self._firestore_enabled():
            firestore.client().collection("users").document(uid).set(data, merge=True)
        else:
            _profiles[uid] = data

        return UserProfile(**data)

    def get_settings(self, uid: str) -> ProfileSettings:
        if self._firestore_enabled():
            data = firestore.client().collection("users").document(uid).collection("settings").document("profile").get().to_dict() or {}
        else:
            data = _settings.get(uid, {})
        return ProfileSettings(**data)

    def update_settings(self, uid: str, updates: dict[str, Any]) -> ProfileSettings:
        current = self.get_settings(uid).model_dump()
        current.update(updates)
        if self._firestore_enabled():
            firestore.client().collection("users").document(uid).collection("settings").document("profile").set(current, merge=True)
        else:
            _settings[uid] = current
        return ProfileSettings(**current)
