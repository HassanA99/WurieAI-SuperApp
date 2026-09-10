from typing import Dict, Any, List

class ProviderService:
    """Production-oriented provider lookup and registration service skeleton.

    This service is intentionally simple and deterministic so the repo can grow
    into a real Firestore/Cloud Run provider service.
    """

    def __init__(self, datastore=None):
        self.datastore = datastore

    def search_providers(self, city: str = "", profession: str = "") -> List[Dict[str, Any]]:
        """Return a list of provider records. Use Firestore or a backend repository in production."""
        return [
            {
                "providerId": "provider-001",
                "name": "Abu",
                "profession": profession or "Plumber",
                "city": city or "Freetown",
                "rating": 4.8,
                "experience": "6 years",
                "available": True,
                "verificationStatus": "verified",
            }
        ]

    def register_provider(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Persist a provider request and create a pending verification record."""
        provider_record = {
            "providerId": payload.get("providerId", "provider-001"),
            "name": payload.get("name"),
            "profession": payload.get("profession"),
            "city": payload.get("city"),
            "experience": payload.get("experience"),
            "verificationStatus": "pending",
        }
        if self.datastore is not None:
            self.datastore[provider_record["providerId"]] = provider_record
        return {
            "status": "success",
            "message": "Provider registration submitted for verification.",
            "provider": provider_record,
        }

    def approve_provider(self, provider_id: str) -> Dict[str, Any]:
        """Approve a provider and return the updated verification state."""
        provider_record = (self.datastore or {}).get(provider_id, {})
        if not provider_record:
            provider_record = {
                "providerId": provider_id,
                "name": "Unknown Provider",
                "profession": "General",
                "city": "Unknown",
                "experience": "N/A",
                "verificationStatus": "approved",
            }
        provider_record["verificationStatus"] = "approved"
        if self.datastore is not None:
            self.datastore[provider_id] = provider_record
        return provider_record
