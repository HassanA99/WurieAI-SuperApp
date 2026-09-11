from __future__ import annotations

import json
from datetime import datetime, timezone

from app.services.service_contracts import AgentResponse, ServiceDomain


class BookingService:
    """Domain adapter for booking creation and service workflow state."""

    VALID_STATUSES = {"created", "confirmed", "in_progress", "completed", "cancelled"}

    def __init__(self, datastore=None):
        self.datastore = datastore if datastore is not None else {}

    @staticmethod
    def _timestamp() -> str:
        return datetime.now(timezone.utc).isoformat()

    def create_booking(self, request: dict | str) -> AgentResponse:
        payload = json.loads(request) if isinstance(request, str) else request
        booking_id = payload.get("booking_id") or payload.get("bookingId")
        if not booking_id:
            booking_id = f"booking-{len(self.datastore) + 1:03d}"

        booking = {
            "bookingId": booking_id,
            "userId": payload.get("user_id") or payload.get("userId"),
            "providerId": payload.get("provider_id") or payload.get("providerId"),
            "serviceType": payload.get("service_type") or payload.get("serviceType"),
            "city": payload.get("city"),
            "location": payload.get("location"),
            "scheduledTime": payload.get("scheduled_time") or payload.get("scheduledTime"),
            "notes": payload.get("notes"),
            "status": "created",
            "statusHistory": [{"status": "created", "updatedAt": self._timestamp()}],
        }
        self.datastore[booking_id] = booking
        return AgentResponse(
            text="I can help you book a service provider. I will collect the request and create a booking workflow.",
            action="BOOK_SERVICE",
            target="Booking",
            data=booking,
            service=ServiceDomain.BOOKING,
            workflow_id=booking_id,
        )

    def list_bookings(self, user_id: str | None = None) -> list[dict]:
        records = list(self.datastore.values()) if hasattr(self.datastore, "values") else list(self.datastore.values())
        if user_id:
            return [record for record in records if record.get("userId") == user_id]
        return records

    def update_status(self, booking_id: str, status: str) -> dict:
        status = str(status).lower()
        if status not in self.VALID_STATUSES:
            raise ValueError(f"status must be one of: {sorted(self.VALID_STATUSES)}")

        booking = self.datastore.get(booking_id, {}) if hasattr(self.datastore, "get") else self.datastore.get(booking_id, {})
        if not booking:
            raise KeyError(f"booking '{booking_id}' not found")

        booking["status"] = status
        history = booking.setdefault("statusHistory", [])
        history.append({"status": status, "updatedAt": self._timestamp()})
        self.datastore[booking_id] = booking
        return booking
