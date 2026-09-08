from __future__ import annotations

from app.services.service_contracts import AgentResponse, ServiceDomain


class BookingService:
    """Domain adapter for booking creation and service workflow state."""

    def create_booking(self, query: str) -> AgentResponse:
        return AgentResponse(
            text="I can help you book a service provider. I will collect the request and create a booking workflow.",
            action="BOOK_SERVICE",
            target="Booking",
            data={"request": query, "status": "created"},
            service=ServiceDomain.BOOKING,
        )
