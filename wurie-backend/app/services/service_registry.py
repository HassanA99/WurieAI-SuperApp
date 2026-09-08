from __future__ import annotations

from typing import Dict, List

from app.services.service_contracts import ServiceDomain


class ServiceRegistry:
    """Static registry describing the cross-domain services WurieAI must support.

    This registry intentionally stays lightweight and deterministic so the
    agent router and Cloud Run API can evolve around a single contract.
    """

    def __init__(self) -> None:
        self.services: Dict[ServiceDomain, Dict[str, object]] = {
            ServiceDomain.MARKET: {
                "name": "market-service",
                "owner": "market-agent",
                "database": "firestore-market-prices",
                "needs": ["commodity_price", "commodity_lookup", "market_intelligence"],
            },
            ServiceDomain.PROVIDER: {
                "name": "provider-service",
                "owner": "provider-agent",
                "database": "firestore-provider-directory",
                "needs": ["provider_search", "provider_registration", "provider_verification"],
            },
            ServiceDomain.BOOKING: {
                "name": "booking-service",
                "owner": "booking-agent",
                "database": "firestore-bookings",
                "needs": ["booking_create", "booking_status", "booking_tracking"],
            },
            ServiceDomain.RIDE: {
                "name": "ride-service",
                "owner": "ride-agent",
                "database": "firestore-rides",
                "needs": ["ride_request", "ride_tracking", "logistics_match"],
            },
            ServiceDomain.WALLET: {
                "name": "wallet-service",
                "owner": "wallet-agent",
                "database": "firestore-wallet-ledger",
                "needs": ["wallet_balance", "wallet_transactions", "wallet_payment"],
            },
            ServiceDomain.ADMIN: {
                "name": "admin-service",
                "owner": "admin-dashboard",
                "database": "firestore-admin-analytics",
                "needs": ["compliance", "provider_review", "dashboard_reporting"],
            },
            ServiceDomain.GENERAL: {
                "name": "assistant-service",
                "owner": "general-agent",
                "database": "firestore-chat-history",
                "needs": ["conversation", "fallback_answer", "recommendation"],
            },
        }

    def supported_domains(self) -> List[str]:
        return [domain.value for domain in self.services.keys()]

    def get_service_definition(self, domain: ServiceDomain) -> Dict[str, object]:
        return self.services.get(domain, self.services[ServiceDomain.GENERAL])
