"""Provider service contract and demo provider matching layer."""

from __future__ import annotations

from app.services.service_contracts import AgentResponse, ServiceDomain


class ProviderService:
    """Structured provider lookup and registration service adapter.

    The service accepts natural language or structured provider queries and returns
    canonical AgentResponse objects for the domain router and UI actions.
    """

    def __init__(self, datastore=None):
        self.datastore = datastore if datastore is not None else {}

    def find(self, query: str) -> AgentResponse:
        """Return a structured provider recommendation.

        Production direction:
        - Use Firestore or a vector database to match provider profiles to user intent.
        - Return ranked provider records with trade, location, rating, and verification.
        """
        text = query.lower()
        if "plumber" in text or "pipe" in text or "leak" in text:
            return AgentResponse(
                text="I found a great plumber nearby. Abu has a 4.8 star rating and can fix your leaking pipe. Would you like to chat with him?",
                action="NAVIGATE_TO",
                target="Hire Artisan:Plumber",
                data={
                    "provider_name": "Abu",
                    "profession": "Plumber",
                    "rating": 4.8,
                    "location": "nearby",
                    "trade": "Plumber",
                    "verified": True,
                },
                service=ServiceDomain.PROVIDER,
            )

        return AgentResponse(
            text="I can help you find a skilled artisan. What kind of work do you need done?",
            action="NAVIGATE_TO",
            target="Hire Artisan",
            data={},
            service=ServiceDomain.PROVIDER,
        )

    def search_providers(self, city: str = "", profession: str = "") -> list[dict]:
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

    def register_provider(self, payload: dict) -> dict:
        """Persist a provider request and create a pending verification record."""
        provider_id = payload.get("providerId", f"provider-{len(self.datastore) + 1:03d}")
        provider_record = {
            "providerId": provider_id,
            "name": payload.get("name"),
            "profession": payload.get("profession"),
            "city": payload.get("city"),
            "experience": payload.get("experience"),
            "verificationStatus": "pending",
        }
        self.datastore[provider_record["providerId"]] = provider_record
        return {
            "status": "success",
            "message": "Provider registration submitted for verification.",
            "provider": provider_record,
        }

    def list_pending_providers(self) -> list[dict]:
        """Return all provider submissions still awaiting admin approval."""
        return [
            provider for provider in self.datastore.values()
            if str(provider.get("verificationStatus", "")).lower() == "pending"
        ]

    def approve_provider(self, provider_id: str) -> dict:
        """Approve a provider and return the updated verification state."""
        provider_record = self.datastore.get(provider_id, {})
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
        self.datastore[provider_id] = provider_record
        return provider_record

    def reject_provider(self, provider_id: str, reason: str | None = None) -> dict:
        """Reject a provider submission and keep the record for admin review."""
        provider_record = self.datastore.get(provider_id, {})
        if not provider_record:
            provider_record = {
                "providerId": provider_id,
                "name": "Unknown Provider",
                "profession": "General",
                "city": "Unknown",
                "experience": "N/A",
                "verificationStatus": "rejected",
            }
        provider_record["verificationStatus"] = "rejected"
        if reason:
            provider_record["rejectionReason"] = reason
        self.datastore[provider_id] = provider_record
        return provider_record


def find_provider(query: str) -> dict:
    """Backward-compatible helper wrapper for the older provider module shape."""
    return ProviderService().find(query).model_dump()
