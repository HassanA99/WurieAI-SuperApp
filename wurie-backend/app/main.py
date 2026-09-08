import os
from fastapi import FastAPI, Depends, HTTPException, Header
from pydantic import BaseModel
import firebase_admin
from firebase_admin import auth, credentials
from app.agents.orchestrator import run_orchestrator
from app.services.domain_router import DomainRouter
from app.services.service_contracts import AgentResponse
from app.services.auth import verify_firebase_token
from app.services.firestore_service_adapters import FirestoreServiceAdapters

# Initialize FastAPI
app = FastAPI(title="WurieAI Backend", version="1.0.0")
router = DomainRouter()
adapters = FirestoreServiceAdapters()

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

class ProviderRegistrationRequest(BaseModel):
    name: str
    trade: str
    location: str
    experience: str

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
