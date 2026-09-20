import logging
import os

from fastapi import Depends, FastAPI, Header, HTTPException, Query
from pydantic import BaseModel, Field

from app.services.domain_router import DomainRouter
from app.services.service_contracts import AgentResponse, CommentItem, LikeState, NotificationItem
from app.services.service_contracts import ProfileSettings, UserProfile
from app.services.auth import auth_health, firebase_admin_ready, initialize_firebase_admin, verify_firebase_token
from app.services.firestore_service_adapters import FirestoreServiceAdapters
from app.services.market_service import MarketService
from app.services.profile_service import ProfileService
from app.services.social_service import SocialService
from app.services.tracing import trace_chat_flow

from fastapi.middleware.cors import CORSMiddleware

logger = logging.getLogger("wurieai.api")
logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)

# --- Runtime bootstrap ------------------------------------------------------------
# Firebase Admin is initialized here, *before* any service repository is constructed.
# A repository decides at construction time whether it binds to Firestore or falls back
# to per-process memory, and that decision is what /health reports as `storage`. Running
# the bootstrap after the repositories are built leaves production healthy and
# authenticated while silently discarding every write.
SENTRY_DSN = os.getenv("SENTRY_DSN", "").strip()
_ERROR_TRACKING_ENABLED = False


def _init_error_tracking() -> bool:
    """Enable error tracking when a DSN is configured; never block boot without one."""
    global _ERROR_TRACKING_ENABLED

    if not SENTRY_DSN:
        _ERROR_TRACKING_ENABLED = False
        logger.warning(
            "SENTRY_DSN is not configured: unhandled errors will only appear in Render logs"
        )
        return False

    try:
        import sentry_sdk
        from sentry_sdk.integrations.fastapi import FastApiIntegration
        from sentry_sdk.integrations.starlette import StarletteIntegration

        environment = os.getenv("SENTRY_ENVIRONMENT", "production")
        sentry_sdk.init(
            dsn=SENTRY_DSN,
            environment=environment,
            release=os.getenv("SENTRY_RELEASE") or None,
            traces_sample_rate=float(os.getenv("SENTRY_TRACES_SAMPLE_RATE", "0.1")),
            integrations=[StarletteIntegration(), FastApiIntegration()],
        )
        _ERROR_TRACKING_ENABLED = True
        logger.info("Error tracking enabled via Sentry (environment=%s)", environment)
        return True
    except Exception:
        _ERROR_TRACKING_ENABLED = False
        logger.exception("Sentry initialisation failed; continuing without error tracking")
        return False


def startup() -> None:
    """Initialize Firebase Admin and error tracking, reporting rather than hiding failures."""
    try:
        initialize_firebase_admin()
        logger.info("Firebase Admin initialized")
    except Exception:
        # The API stays bootable without Firebase, but every write then falls back to
        # in-memory storage, so the failure must be loud and diagnosable.
        logger.exception(
            "Firebase Admin initialization failed: storage will use the in-memory fallback"
        )

    _init_error_tracking()


startup()

# Initialize FastAPI
app = FastAPI(title="WurieAI Backend", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
router = DomainRouter()
adapters = FirestoreServiceAdapters()
profile_service = ProfileService()
provider_service = adapters.provider_service

if firebase_admin_ready() and not adapters.providers.uses_firestore:
    # Credentials work but the repositories still bound to memory, which means the
    # bootstrap ran after they were built. Every provider, booking and wallet write
    # would be lost on restart, so this must be visible in the logs, not just /health.
    logger.error(
        "Firebase Admin is ready but repositories are in-memory: writes will not persist"
    )
ADMIN_ROLES = {"admin", "staff", "super_admin"}

# --- Agent runtime availability ---------------------------------------------------
# Imported defensively so the API still boots (and reports why) when the agent
# dependencies or model credentials are missing. Degradation is never silent: the
# reason is logged, exposed on /health, and marked on degraded chat responses.
AGENT_FALLBACK_MODE = os.getenv("WURIE_AGENT_FALLBACK", "router").strip().lower()
AGENT_IMPORT_ERROR: Exception | None = None
try:
    from app.agents.orchestrator import AgentError, agent_status, run_orchestrator
except Exception as exc:  # pragma: no cover - depends on the installed environment
    AGENT_IMPORT_ERROR = exc
    run_orchestrator = None
    agent_status = None
    AgentError = RuntimeError  # alias only; unreachable while the import is broken
    logger.exception("Agent runtime unavailable: orchestrator import failed")


def agent_health() -> dict:
    """Report agent readiness without ever failing the health check."""
    if run_orchestrator is None:
        return {
            "available": False,
            "reason": f"import_failed: {AGENT_IMPORT_ERROR}",
            "fallback": AGENT_FALLBACK_MODE,
        }
    status = dict(agent_status() if agent_status else {"available": False})
    status["fallback"] = AGENT_FALLBACK_MODE
    return status


def storage_health() -> dict:
    """Report where data is actually written and whether Firebase Admin is live.

    Without credentials every write silently lands in per-process memory, so this is
    the signal that distinguishes a real production deployment from a demo one.
    """
    uses_firestore = any(
        repository.uses_firestore
        for repository in (adapters.providers, adapters.bookings, adapters.wallets)
    )
    return {
        "firebase_admin": firebase_admin_ready(),
        "storage": "firestore" if uses_firestore else "memory",
    }


class ChatRequest(BaseModel):
    message: str
    location: str | None = None

class ChatResponse(BaseModel):
    text: str
    action: str | None = None
    target: str | None = None
    data: dict = Field(default_factory=dict)
    service: str | None = None
    workflow_id: str | None = None

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


async def firebase_dependency(
    authorization: str | None = Header(default=None),
    x_firebase_appcheck: str | None = Header(default=None),
):
    """Keep the public FastAPI route using the shared token verification policy.

    App Check is verified here rather than per route, so every authenticated endpoint
    inherits the same trust policy as soon as WURIE_APP_CHECK_ENFORCE is enabled.
    """
    return await verify_firebase_token(authorization, x_firebase_appcheck)


def require_admin_role(user: dict) -> None:
    role = (user.get("claims") or {}).get("role")
    if role not in ADMIN_ROLES:
        raise HTTPException(status_code=403, detail="Admin access required")

@app.post("/chat", response_model=ChatResponse)
@app.post("/api/chat", response_model=ChatResponse)
@app.post("/api/v1/chat", response_model=ChatResponse)
@trace_chat_flow
async def chat_endpoint(request: ChatRequest, user=Depends(firebase_dependency)):
    """Main assistant endpoint.

    Prefers the LangGraph agent runtime. When that runtime cannot answer, the request
    is served by the deterministic domain router and the response is explicitly marked
    as degraded, so a broken AI path is visible to clients and operators instead of
    silently changing behaviour.
    """
    uid = user.get("uid", "unknown")

    if run_orchestrator is not None:
        try:
            result = run_orchestrator(request.message, uid)
            text = (result.get("text") or "").strip()
            if not text:
                raise AgentError("agent returned an empty response")

            return ChatResponse(
                text=text,
                action=result.get("action"),
                target=result.get("target"),
                data=result.get("data") or {},
                service=result.get("service"),
                workflow_id=result.get("workflow_id"),
            )
        except AgentError as exc:
            reason = f"agent_error: {exc}"
            logger.warning("Agent could not answer user=%s: %s", uid, exc)
        except Exception as exc:
            reason = f"agent_exception: {exc}"
            logger.exception("Unexpected agent failure user=%s", uid)
    else:
        reason = f"agent_unavailable: {AGENT_IMPORT_ERROR}"
        logger.error("Agent runtime was not imported; cannot serve user=%s", uid)

    if AGENT_FALLBACK_MODE == "error":
        raise HTTPException(status_code=503, detail="Assistant is temporarily unavailable")

    result = router.route(request.message)
    logger.warning("Degraded response served by domain router user=%s reason=%s", uid, reason)

    if isinstance(result, AgentResponse):
        data = dict(result.data or {})
        data["degraded"] = True
        data["degraded_reason"] = reason
        return ChatResponse(
            text=result.text,
            action=result.action,
            target=result.target,
            data=data,
            service=result.service.value,
            workflow_id=result.workflow_id,
        )

    data = dict(result.get("data") or {})
    data["degraded"] = True
    data["degraded_reason"] = reason
    return ChatResponse(
        text=result.get("text", "I'm sorry, I couldn't process that."),
        action=result.get("action"),
        target=result.get("target"),
        data=data,
        service=result.get("service"),
        workflow_id=result.get("workflow_id"),
    )

@app.get("/health")
def health_check():
    """Liveness plus runtime readiness, so a broken deployment is visible from outside.

    Kept flat and string-valued on purpose: the Android client declares this response
    as ``Map<String, String>``, and the platform health check only reads the status
    code. Structured detail is available on ``/api/v1/runtime/status``.
    """
    status = agent_health()
    storage = storage_health()
    auth = auth_health()
    return {
        "status": "healthy",
        "agent_available": "true" if status.get("available") else "false",
        "agent_model": str(status.get("model") or ""),
        "agent_fallback": str(status.get("fallback") or ""),
        "agent_reason": str(status.get("reason") or ""),
        "firebase_admin": "true" if storage["firebase_admin"] else "false",
        "storage": str(storage["storage"]),
        "error_tracking": "sentry" if _ERROR_TRACKING_ENABLED else "off",
        "auth_mode": str(auth["mode"]),
        "app_check": str(auth["app_check"]),
    }


@app.get("/api/v1/runtime/status")
def runtime_status_endpoint(user=Depends(firebase_dependency)):
    """Structured runtime readiness for operators and dashboards."""
    return {
        "agent": agent_health(),
        "storage": storage_health(),
        "auth": auth_health(),
        "error_tracking": "sentry" if _ERROR_TRACKING_ENABLED else "off",
    }


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
