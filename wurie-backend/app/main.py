import os
from fastapi import FastAPI, Depends, HTTPException, Header, Query
from pydantic import BaseModel
import firebase_admin
from firebase_admin import auth, credentials
from app.agents.orchestrator import run_orchestrator
from app.services.domain_router import DomainRouter
from app.services.service_contracts import AgentResponse, CommentItem, LikeState, NotificationItem
from app.services.service_contracts import ProfileSettings, UserProfile
from app.services.auth import initialize_firebase_admin, verify_firebase_token
from app.services.firestore_service_adapters import FirestoreServiceAdapters
from app.services.profile_service import ProfileService
from app.services.social_service import SocialService
from app.services.tracing import trace_chat_flow

# Initialize FastAPI
app = FastAPI(title="WurieAI Backend", version="1.0.0")
router = DomainRouter()
adapters = FirestoreServiceAdapters()
profile_service = ProfileService()
provider_service = adapters.provider_service
ADMIN_ROLES = {"admin", "staff", "super_admin"}


def startup() -> None:
    """Initialize Firebase on backend startup when project credentials are available."""
    try:
        initialize_firebase_admin()
    except Exception:
        # Keep the backend bootable in local/demo mode when Firebase is not configured yet.
        pass


startup()

# Initialize Firebase Admin (Uncomment and configure with your service account key in production)
# cred = credentials.Certificate("path/to/serviceAccountKey.json")
# firebase_admin.initialize_app(cred)

class ChatRequest(BaseModel):
    message: str
    location: str | None = None

class ChatResponse(BaseModel):
    text: str
    action: str | None = None
    target: str | None = None

class MarketPriceRequest(BaseModel):
    commodity: str
    location: str | None = None

class MarketPriceResponse(BaseModel):
    text: str
    action: str | None = None
    target: str | None = None
    data: dict | None = None

class WalletBalanceResponse(BaseModel):
    userId: str
    balance: float
    currency: str = "USD"
    transactions: list[dict] = []

class BookingRequest(BaseModel):
    user_id: str
    provider_id: str
    service_type: str
    city: str
    location: str | None = None
    scheduled_time: str | None = None
    notes: str | None = None

class BookingResponse(BaseModel):
    booking_id: str
    status: str
    message: str


class ProfileUpdateRequest(BaseModel):
    full_name: str | None = None
    phone: str | None = None
    city: str | None = None


class ProfileSettingsUpdateRequest(BaseModel):
    notifications_enabled: bool | None = None
    biometric_enabled: bool | None = None
    push_enabled: bool | None = None
    offline_cache_enabled: bool | None = None
    language: str | None = None


class CommentCreateRequest(BaseModel):
    body: str
    username: str = "Wurie User"


class NotificationCreateRequest(BaseModel):
    title: str
    body: str
    category: str = "general"


async def firebase_dependency(authorization: str | None = Header(default=None)):
    """Keep the public FastAPI route using the shared token verification policy."""
    return await verify_firebase_token(authorization)


def require_admin_role(user: dict) -> None:
    role = (user.get("claims") or {}).get("role")
    if role not in ADMIN_ROLES:
        raise HTTPException(status_code=403, detail="Admin access required")

@app.post("/api/v1/chat", response_model=ChatResponse)
@trace_chat_flow
async def chat_endpoint(request: ChatRequest, user=Depends(firebase_dependency)):
    """
    Main endpoint for the Android app.

    Keeps the existing Android response shape while routing through the
    structured domain service router. In production, this should be backed
    by Firestore and service adapters, while the LangGraph orchestrator can
    sit behind or complement this routing layer.
    """
    try:
        # Prefer the structured domain router for service-aware, business-safe flows.
        # Fall back to the LangGraph orchestrator if needed in production.
        result = router.route(request.message)

        if isinstance(result, AgentResponse):
            return ChatResponse(
                text=result.text,
                action=result.action,
                target=result.target,
            )

        # Maintain compatibility with the old orchestrator result shape.
        return ChatResponse(
            text=result.get("text", "I'm sorry, I couldn't process that."),
            action=result.get("action"),
            target=result.get("target")
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
def health_check():
    return {"status": "healthy"}


@app.get("/api/v1/profile", response_model=UserProfile)
async def get_profile(user=Depends(firebase_dependency)):
    """Return the profile belonging to the verified Firebase user."""
    return profile_service.get_profile(user["uid"], user.get("claims", {}).get("email", ""))


@app.patch("/api/v1/profile", response_model=UserProfile)
async def update_profile(request: ProfileUpdateRequest, user=Depends(firebase_dependency)):
    """Update only the profile owned by the verified Firebase user."""
    return profile_service.update_profile(
        user["uid"],
        request.model_dump(exclude_none=True),
        user.get("claims", {}).get("email", ""),
    )


@app.get("/api/v1/profile/settings", response_model=ProfileSettings)
async def get_profile_settings(user=Depends(firebase_dependency)):
    return profile_service.get_settings(user["uid"])


@app.patch("/api/v1/profile/settings", response_model=ProfileSettings)
async def update_profile_settings(request: ProfileSettingsUpdateRequest, user=Depends(firebase_dependency)):
    return profile_service.update_settings(user["uid"], request.model_dump(exclude_none=True))


@app.get("/api/v1/social/comments", response_model=list[CommentItem])
async def list_comments(user=Depends(firebase_dependency)):
    return SocialService().list_comments(user["uid"])


@app.post("/api/v1/social/comments", response_model=CommentItem)
async def add_comment(request: CommentCreateRequest, user=Depends(firebase_dependency)):
    return SocialService().add_comment(user["uid"], request.body, username=request.username)


@app.get("/api/v1/social/notifications", response_model=list[NotificationItem])
async def list_notifications(user=Depends(firebase_dependency)):
    return SocialService().list_notifications(user["uid"])


@app.post("/api/v1/social/notifications", response_model=NotificationItem)
async def add_notification(request: NotificationCreateRequest, user=Depends(firebase_dependency)):
    return SocialService().add_notification(user["uid"], request.title, request.body, category=request.category)


@app.patch("/api/v1/social/notifications/{notification_id}/read", response_model=NotificationItem)
async def mark_notification_read(notification_id: str, user=Depends(firebase_dependency)):
    return SocialService().mark_notification_read(user["uid"], notification_id)


@app.post("/api/v1/social/comments/{comment_id}/like", response_model=LikeState)
async def toggle_comment_like(comment_id: str, user=Depends(firebase_dependency)):
    return SocialService().toggle_like(user["uid"], comment_id)


class ProviderRegistrationRequest(BaseModel):
    name: str
    trade: str
    location: str
    experience: str


class ProviderApprovalRequest(BaseModel):
    reason: str | None = None


class BookingStatusRequest(BaseModel):
    status: str


@app.get("/api/v1/providers/pending")
async def get_pending_providers(user=Depends(firebase_dependency)):
    """Return all pending verification requests for the admin dashboard."""
    require_admin_role(user)
    return provider_service.list_pending_providers()


@app.get("/api/v1/bookings")
async def list_bookings(user=Depends(firebase_dependency), user_id: str | None = None):
    """Return booking records for the current user or a provided uid."""
    target_user = user_id or user.get("uid")
    if not target_user:
        raise HTTPException(status_code=400, detail="user_id is required")
    if user.get("claims", {}).get("role") in ADMIN_ROLES or target_user == user.get("uid"):
        return adapters.booking_service.list_bookings(target_user)
    raise HTTPException(status_code=403, detail="Not allowed to view these bookings")


@app.get("/api/v1/providers")
async def search_providers(
    city: str = Query(default=""),
    profession: str = Query(default=""),
    user=Depends(firebase_dependency),
):
    """Return approved providers for the customer hiring flow."""
    return provider_service.search_providers(city=city, profession=profession)


@app.post("/api/v1/providers/{provider_id}/approve")
async def approve_provider(provider_id: str, request: ProviderApprovalRequest | None = None, user=Depends(firebase_dependency)):
    """Approve a provider record for the admin dashboard."""
    require_admin_role(user)
    return provider_service.approve_provider(provider_id)


@app.post("/api/v1/providers/{provider_id}/reject")
async def reject_provider(provider_id: str, request: ProviderApprovalRequest | None = None, user=Depends(firebase_dependency)):
    """Reject a provider record for the admin dashboard."""
    require_admin_role(user)
    return provider_service.reject_provider(provider_id, request.reason if request else None)


@app.post("/api/v1/market/price", response_model=ChatResponse)
async def market_price(request: MarketPriceRequest, user=Depends(firebase_dependency)):
    """Return a structured market-price answer from the market service."""
    try:
        service = MarketService()
        result = service.lookup(request.commodity)
        return ChatResponse(
            text=result.text,
            action=result.action,
            target=result.target,
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

@app.post("/api/v1/bookings", response_model=BookingResponse)
async def create_booking(request: BookingRequest, user=Depends(firebase_dependency)):
    """Create a booking workflow and return a canonical backend response."""
    try:
        payload = request.model_dump()
        payload["user_id"] = user.get("uid", request.user_id)
        result = adapters.booking_service.create_booking(payload)
        return BookingResponse(
            booking_id=result.workflow_id or result.data["bookingId"],
            status=result.data["status"],
            message=result.text,
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/api/v1/bookings/{booking_id}/status")
async def update_booking_status(booking_id: str, request: BookingStatusRequest, user=Depends(firebase_dependency)):
    """Update a booking lifecycle state for the authenticated user or admin."""
    booking = adapters.booking_service.datastore.get(booking_id, {})
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found")
    if user.get("claims", {}).get("role") in ADMIN_ROLES or booking.get("userId") == user.get("uid"):
        updated = adapters.booking_service.update_status(booking_id, request.status)
        return {"bookingId": booking_id, "status": updated["status"], "statusHistory": updated.get("statusHistory", [])}
    raise HTTPException(status_code=403, detail="Not allowed to update this booking")


@app.get("/api/v1/wallet/balance", response_model=WalletBalanceResponse)
async def wallet_balance(user=Depends(firebase_dependency)):
    """Return a wallet balance response for the mobile repository contract."""
    try:
        service = adapters.wallet_service
        result = service.balance("wallet")
        balance = service.get_balance(user.get("uid", "unknown"))
        return WalletBalanceResponse(
            userId=user.get("uid", "unknown"),
            balance=float(balance.get("balance", 150.0)),
            currency=str(balance.get("currency", "USD")),
            transactions=list(balance.get("transactions", [])),
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

@app.post("/api/v1/providers/register")
async def register_provider(request: ProviderRegistrationRequest, user=Depends(verify_firebase_token)):
    """Submit a provider profile for verification."""
    result = provider_service.register_provider(
        {
            "name": request.name,
            "profession": request.trade,
            "city": request.location,
            "experience": request.experience,
        }
    )
    provider = result["provider"]
    return {
        "status": result["status"],
        "message": result["message"],
        "providerId": provider["providerId"],
    }
