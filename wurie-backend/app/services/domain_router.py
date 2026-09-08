from __future__ import annotations

from app.services.service_contracts import AgentResponse, ServiceDomain
from app.services.market_service import MarketService
from app.services.provider_service import ProviderService
from app.services.booking_service import BookingService
from app.services.ride_service import RideService
from app.services.wallet_service import WalletService


class DomainRouter:
    """Domain router/service dispatcher for WurieAI multi-agent flow.

    This keeps a single entry point for question classification and forwards
    to the correct product domain. It also returns a structured AgentResponse.
    """

    def __init__(self) -> None:
        self.market_service = MarketService()
        self.provider_service = ProviderService()
        self.booking_service = BookingService()
        self.ride_service = RideService()
        self.wallet_service = WalletService()

    def route(self, message: str) -> AgentResponse:
        lower = message.lower()

        if any(keyword in lower for keyword in ["price", "market", "rice", "fish", "cement"]):
            return self.market_service.lookup(message)

        if any(keyword in lower for keyword in ["artisan", "provider", "plumber", "electrician", "mechanic", "repair", "hire"]):
            return self.provider_service.find(message)

        if any(keyword in lower for keyword in ["book", "booking", "schedule", "appointment"]):
            return self.booking_service.create_booking(message)

        if any(keyword in lower for keyword in ["ride", "transport", "driver", "taxi", "keke", "trip"]):
            return self.ride_service.request_ride(message)

        if any(keyword in lower for keyword in ["wallet", "balance", "pay", "money", "transaction"]):
            return self.wallet_service.balance(message)

        return AgentResponse(
            text="I am WurieAI, your local commerce and service assistant. How can I help you today?",
            action=None,
            target=None,
            data={"message": message},
            service=ServiceDomain.GENERAL,
        )
