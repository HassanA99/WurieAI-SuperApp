# WurieAI Architecture

## Product overview
WurieAI is a regional AI super app for the Mano River Union, combining market discovery, service-provider marketplace, booking, wallet, and assistant workflows.

## Target platform
- Android client: Jetpack Compose
- Backend: FastAPI on Google Cloud Run
- Identity: Firebase Authentication
- Data: Firestore
- AI: Gemini with structured function calling and agent orchestration
- Notifications: Firebase Cloud Messaging
- Storage: Firebase Cloud Storage
- Observability: Cloud Logging and Monitoring

## Current repository map

### Android app
- Main entrypoint: `app/src/main/java/com/example/MainActivity.kt`
- Chat and Gemini tool-calling: `app/src/main/java/com/example/viewmodel/ChatViewModel.kt`
- Auth and Firebase flows: `app/src/main/java/com/example/viewmodel/AuthViewModel.kt`

### Backend
- API entrypoint: `wurie-backend/app/main.py`
- Agent orchestration: `wurie-backend/app/agents/orchestrator.py`
- Market mock agent: `wurie-backend/app/agents/market_agent.py`
- Artisan mock agent: `wurie-backend/app/agents/artisan_agent.py`

## Proposed production architecture

### Mobile and client
- Compose screens for onboarding, auth, profile, explore, assistant, market, provider, booking, wallet, activity, and provider/admin dashboards.
- Firebase App Check and secure client configuration.
- Backend contract must treat all chat and provider data as structured JSON data, never freeform UI assumptions.

### API and orchestration
- Cloud Run hosts a single FastAPI public API.
- Public endpoints must require a validated Firebase user token.
- The chat endpoint sends structured user intent to the agent router.
- Specialist agents produce structured outputs with `text`, `action`, `target`, and optional `data` fields.

### Data layer
- Firestore collections: `users`, `providers`, `markets`, `bookings`, `wallets`, `chats`, `notifications`.
- Cloud Storage for provider images and docs.
- Optional Cloud SQL for advanced relational data analytics.
- Optional vector database for semantic provider matching.

## Security
- Never hardcode Gemini keys in the Android app.
- Store all credentials in Secret Manager.
- Use Firebase App Check.
- Enforce Firestore Security Rules by role.
- Verify Firebase tokens on every protected backend route.
- Use least-privilege service accounts.

## Roadmap

### Phase 1
- Finalize product scope and roles.
- Create Firestore schema and data contracts.
- Define provider, market, booking, and wallet collections.

### Phase 2
- Replace demo agents with Firestore-backed services.
- Build provider registration and verification endpoints.
- Deploy FastAPI backend to Cloud Run.

### Phase 3
- Build provider discovery, market price, and booking flows.
- Add chat service integration to Gemini.
- Add notifications and dashboards.

### Phase 4
- Add analytics, observability, and production quality gates.
- Build CI/CD and automated integration tests.
