# WurieAI Product Roadmap

## Milestone 1 — Product foundation
- Confirm MVP scope: market price, provider discovery, booking, wallet, chat.
- Define user roles: customer, provider, admin, logistics partner.
- Define service categories: market, provider, ride, delivery, wallet.

## Milestone 2 — Identity and profile
- Firebase Auth with Email/Google/Guest.
- Firestore `users` collection.
- Profile completion flow.
- App Check setup.

## Milestone 3 — Provider marketplace
- Provider collection and provider onboarding.
- Provider verification and admin approval.
- Provider search, trade category, geography, price, rating.

## Milestone 4 — Backend and AI
- FastAPI service on Cloud Run.
- Firebase ID token validation.
- Gemini wrapped in a backend `chat` endpoint.
- Chat route returns `text`, `action`, `target`, and `data`.
- Router and specialist agents.

## Milestone 5 — Booking and wallet
- Booking creation and status flow.
- Wallet and transaction flow.
- Notifications and activity logs.

## Milestone 6 — Launch quality
- Monitoring and health checks.
- Firestore security testing.
- Integration test coverage.
- CI/CD pipeline.
