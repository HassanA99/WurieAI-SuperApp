import os
from fastapi import FastAPI, Depends, HTTPException, Header
from pydantic import BaseModel
import firebase_admin
from firebase_admin import auth, credentials
from app.agents.orchestrator import run_orchestrator

# Initialize FastAPI
app = FastAPI(title="WurieAI Backend", version="1.0.0")

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

async def verify_firebase_token(authorization: str = Header(None)):
    """Middleware to verify the Firebase JWT token from the Android app."""
    if not authorization:
        # For local development, we allow requests without auth.
        # In production, raise HTTPException(status_code=401)
        return {"uid": "dev_user"}
    
    try:
        token = authorization.split("Bearer ")[1]
        # decoded_token = auth.verify_id_token(token)
        # return decoded_token
        return {"uid": "verified_user"}
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid authentication credentials")

@app.post("/api/v1/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest, user=Depends(verify_firebase_token)):
    """
    Main endpoint for the Android app. 
    Receives the user message, passes it to the LangGraph Orchestrator, and returns the response.
    """
    try:
        # Run the Multi-Agent Orchestrator
        result = run_orchestrator(request.message, user_id=user["uid"])
        
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
