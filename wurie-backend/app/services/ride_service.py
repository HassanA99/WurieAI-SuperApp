from __future__ import annotations

from app.services.service_contracts import AgentResponse, ServiceDomain


class RideService:
    """Domain adapter for ride and logistics booking flows."""

    def request_ride(self, query: str) -> AgentResponse:
        return AgentResponse(
            text="I found a nearby ride request workflow. I can help you book transport or logistics.",
            action="BOOK_SERVICE",
            target="RideBooking",
            data={"request": query, "status": "ride_requested"},
            service=ServiceDomain.RIDE,
        )
