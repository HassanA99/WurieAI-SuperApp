# WurieAI Product Architecture Blueprint

## Product Mission

WurieAI is an AI-native regional commerce and service coordination platform for the Mano River Union market. It must connect users to local provider discovery, market prices, booking workflows, ride/logistics, wallet flows, and local commerce intelligence.

## Product Domains

The platform should be organized around domain services rather than a single monolithic chatbot experience.

1. Identity and Profile
2. Market Intelligence
3. Service Provider and Artisan Discovery
4. Booking and Fulfillment
5. Ride and Logistics
6. Wallet and Ledger
7. Notifications and Activity
8. Admin and Compliance

## Core Product Flow

1. User signs in using Firebase Auth.
2. User completes profile and role assignment.
3. User asks the assistant a natural-language question.
4. Router agent classifies the question into a product domain.
5. Domain agent or service returns structured business output.
6. UI navigates to the correct service screen or opens a provider chat.

## Data Contracts

Core collections or tables:

- users
- providers
- markets
- bookings
- rides
- wallet
- chats
- activity
- provider_reviews

All structured responses should include:

- text
- action
- target
- data
- service
- workflow_id

## Recommended Stack

- Android Compose app
- Firebase Authentication
- Firestore
- Cloud Run + FastAPI
- Gemini
- LangGraph orchestration
- Google ADK optional later as a specialized agent runtime

## Security Rules

- Require Firebase App Check on mobile clients.
- Verify Firebase identity tokens in Cloud Run.
- Store secrets in Google Secret Manager.
- Use Firestore security rules by role.
- Never expose backend secrets in the Android app.
- Require provider verification before public listing.

## MVP Deliverables

- Sign-in and profile
- Assistant chat
- Market price lookup
- Provider/artisan search
- Provider registration
- Booking workflow
- Wallet screen
- Admin and provider dashboards

## Phase Plan

### Phase 1

Foundation, auth, profile, and Firestore data model.

### Phase 2

Market pricing and provider discovery service integration.

### Phase 3

Booking, ride/logistics, and wallet services.

### Phase 4

Analytics, notifications, provider verification, and admin operations.

## Important Principle

The assistant must route to structured business services. It should not act like a free-text-only page.
