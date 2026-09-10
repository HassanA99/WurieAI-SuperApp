from __future__ import annotations

import re
from typing import Any

import firebase_admin
from firebase_admin import firestore

from app.services.service_contracts import ProfileSettings, UserProfile


_profiles: dict[str, dict[str, Any]] = {}
_settings: dict[str, dict[str, Any]] = {}
_ALLOWED_LANGUAGES = {"en", "fr", "es", "pt"}


class ProfileService:
    """Read and update user-owned profile data.

    Firestore is used when Firebase Admin is initialized. The in-memory store is
    deliberately retained for local tests and development without credentials.
    """

    def _firestore_enabled(self) -> bool:
        return bool(firebase_admin._apps)

    @staticmethod
    def _sanitize_text(value: Any, field_name: str, max_length: int = 120) -> str:
        if value is None:
            return ""
        text = str(value).strip()
        if not text:
            raise ValueError(f"{field_name} cannot be empty")
        sanitized = re.sub(r"\s+", " ", text)
        sanitized = sanitized[:max_length]
        if len(sanitized) < 2:
            raise ValueError(f"{field_name} is too short")
        return sanitized

    @staticmethod
    def _normalize_phone(value: Any) -> str:
        if value is None:
            return ""
        cleaned = re.sub(r"[^\d+\s-]", "", str(value)).strip()
        if not cleaned:
            raise ValueError("phone number cannot be empty")
        if not re.fullmatch(r"\+?[0-9][0-9\s-]{6,20}", cleaned):
            raise ValueError("phone number format is invalid")
        return cleaned

    @staticmethod
    def _normalize_language(value: Any) -> str:
        language = str(value or "en").strip().lower()
        if language not in _ALLOWED_LANGUAGES:
            raise ValueError(f"language must be one of: {sorted(_ALLOWED_LANGUAGES)}")
        return language

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

        cleaned_updates: dict[str, Any] = {}
        for key, value in updates.items():
            if value is None:
                continue
            if key == "full_name":
                cleaned_updates[key] = self._sanitize_text(value, "full_name", 80)
            elif key == "phone":
                cleaned_updates[key] = self._normalize_phone(value)
            elif key == "city":
                cleaned_updates[key] = self._sanitize_text(value, "city", 80)
            elif key in {"role", "email"}:
                cleaned_updates[key] = self._sanitize_text(value, key, 80)
            else:
                cleaned_updates[key] = value

        data.update(cleaned_updates)
        if not data.get("full_name"):
            raise ValueError("full_name cannot be empty")
        if "phone" in data and data.get("phone"):
            data["phone"] = self._normalize_phone(data["phone"])
        if "city" in data and data.get("city"):
            data["city"] = self._sanitize_text(data["city"], "city", 80)
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
        validated = dict(current)

        if "notifications_enabled" in updates and not isinstance(updates["notifications_enabled"], bool):
            raise ValueError("notifications_enabled must be a boolean")
        if "biometric_enabled" in updates and not isinstance(updates["biometric_enabled"], bool):
            raise ValueError("biometric_enabled must be a boolean")
        if "push_enabled" in updates and not isinstance(updates["push_enabled"], bool):
            raise ValueError("push_enabled must be a boolean")
        if "offline_cache_enabled" in updates and not isinstance(updates["offline_cache_enabled"], bool):
            raise ValueError("offline_cache_enabled must be a boolean")
        if "language" in updates:
            validated["language"] = self._normalize_language(updates["language"])

        for key, value in updates.items():
            if key in {"notifications_enabled", "biometric_enabled", "push_enabled", "offline_cache_enabled"}:
                validated[key] = value

        if self._firestore_enabled():
            firestore.client().collection("users").document(uid).collection("settings").document("profile").set(validated, merge=True)
        else:
            _settings[uid] = validated
        return ProfileSettings(**validated)
