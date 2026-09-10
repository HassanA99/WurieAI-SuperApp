import os
from fastapi import FastAPI, Depends, HTTPException, Header
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

# Initialize FastAPI
app = FastAPI(title="WurieAI Backend", version="1.0.0")
router = DomainRouter()
adapters = FirestoreServiceAdapters()
profile_service = ProfileService()


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

@app.post("/api/v1/chat", response_model=ChatResponse)
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


@app.get("/api/v1/providers/pending")
async def get_pending_providers(user=Depends(firebase_dependency)):
    """Return all pending verification requests for the admin dashboard."""
    role = (user.get("claims") or {}).get("role")
    if role and role not in {"admin", "staff", "super_admin"}:
        raise HTTPException(status_code=403, detail="Admin access required")
    return ProviderService().list_pending_providers()


@app.post("/api/v1/providers/{provider_id}/approve")
async def approve_provider(provider_id: str, request: ProviderApprovalRequest | None = None, user=Depends(firebase_dependency)):
    """Approve a provider record for the admin dashboard."""
    role = (user.get("claims") or {}).get("role")
    if role and role not in {"admin", "staff", "super_admin"}:
        raise HTTPException(status_code=403, detail="Admin access required")
    return ProviderService().approve_provider(provider_id)


@app.post("/api/v1/providers/{provider_id}/reject")
async def reject_provider(provider_id: str, request: ProviderApprovalRequest | None = None, user=Depends(firebase_dependency)):
    """Reject a provider record for the admin dashboard."""
    role = (user.get("claims") or {}).get("role")
    if role and role not in {"admin", "staff", "super_admin"}:
        raise HTTPException(status_code=403, detail="Admin access required")
    return ProviderService().reject_provider(provider_id, request.reason if request else None)


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
        service = BookingService()
        result = service.create_booking(request.model_dump_json())
        return BookingResponse(
            booking_id="booking-001",
            status="created",
            message=result.text,
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

@app.get("/api/v1/wallet/balance", response_model=WalletBalanceResponse)
async def wallet_balance(user=Depends(firebase_dependency)):
    """Return a wallet balance response for the mobile repository contract."""
    try:
        service = WalletService()
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
    """
    Registers a new artisan/provider on the platform.
    In production:
    1. Saves the profile to Cloud SQL (PostgreSQL).
    2. Converts the profile text into an Embedding (Vector).
    3. Stores the embedding in pgvector/Pinecone so the ArtisanAgent can match them to users.
    """
    # Simulate database insertion and vectorization
    return {"status": "success", "message": f"Provider {request.name} registered successfully."}
