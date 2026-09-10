from __future__ import annotations

from enum import Enum
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class ServiceDomain(str, Enum):
    """Canonical business domains the assistant can route to."""

    MARKET = "market"
    PROVIDER = "provider"
    BOOKING = "booking"
    RIDE = "ride"
    WALLET = "wallet"
    GENERAL = "general"
    ADMIN = "admin"


class ServiceAction(str, Enum):
    """Structured action hints for the Android or web client."""

    NAVIGATE_TO = "NAVIGATE_TO"
    CHAT_WITH_PROVIDER = "CHAT_WITH_PROVIDER"
    BOOK_SERVICE = "BOOK_SERVICE"
    TRACK_SERVICE = "TRACK_SERVICE"
    SEARCH = "SEARCH"
    SHOW_WALLET = "SHOW_WALLET"
    SHOW_MARKET = "SHOW_MARKET"


class AgentResponse(BaseModel):
    """Production response contract for the assistant backend.

    The mobile UI and web frontend can interpret this as a structured
    app action instead of depending only on free text.
    """

    text: str
    action: Optional[str] = None
    target: Optional[str] = None
    data: Dict[str, Any] = Field(default_factory=dict)
    service: ServiceDomain = ServiceDomain.GENERAL
    workflow_id: Optional[str] = None


class UserProfile(BaseModel):
    uid: str
    full_name: str
    email: str
    phone: Optional[str] = None
    city: Optional[str] = None
    role: str = "customer"
    profile_completed: bool = False


class ProviderProfile(BaseModel):
    provider_id: str
    name: str
    profession: str
    city: str
    area: Optional[str] = None
    experience: Optional[str] = None
    rating: float = 0.0
    verification_status: str = "pending"
    skills: list[str] = Field(default_factory=list)
    availability: str = "available"


class BookingRequest(BaseModel):
    user_id: str
    provider_id: str
    service_type: str
    city: str
    location: Optional[str] = None
    scheduled_time: Optional[str] = None
    notes: Optional[str] = None


class ProfileSettings(BaseModel):
    notifications_enabled: bool = True
    biometric_enabled: bool = False
    push_enabled: bool = True
    offline_cache_enabled: bool = True
    language: str = "en"


class NotificationItem(BaseModel):
    id: str
    title: str
    body: str
    read: bool = False
    category: str = "general"
    created_at: Optional[str] = None
