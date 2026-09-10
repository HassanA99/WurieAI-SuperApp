import os
from fastapi import FastAPI, Depends, HTTPException, Header
from pydantic import BaseModel
import firebase_admin
from firebase_admin import auth, credentials
from app.agents.orchestrator import run_orchestrator
from app.services.domain_router import DomainRouter
from app.services.service_contracts import AgentResponse
from app.services.service_contracts import ProfileSettings, UserProfile
from app.services.auth import initialize_firebase_admin, verify_firebase_token
from app.services.firestore_service_adapters import FirestoreServiceAdapters
from app.services.profile_service import ProfileService

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

class ProviderRegistrationRequest(BaseModel):
    name: str
    trade: str
    location: str
    experience: str

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
