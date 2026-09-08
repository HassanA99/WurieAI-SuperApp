# WurieAI Security and Reliability Plan

## Security goals
WurieAI must protect identity, provider data, bookings, market data, chat history, wallet information, and user profile information.

## Production controls

### Identity and auth
- Use Firebase Authentication for all user identity.
- Require verified Firebase ID tokens for backend access.
- Support email/password, Google identity, and guest fallback only for demo or limited flows.

### Mobile app
- Enable Firebase App Check.
- Keep all production API keys off the client.
- Place the mobile Gemini call behind a backend API endpoint instead of calling Gemini directly from the app.

### Backend
- Store all backend credentials in Google Secret Manager.
- Validate request schemas with Pydantic.
- Add role checks for admin, provider, and user endpoints.
- Add request logging and audit events.

### Data
- Use Firestore security rules that restrict collections by user role and ownership.
- Providers must be verified before appearing in public discovery listings.
- Users must only access their own sensitive records.
- Bookings and payment records must be protected by role-specific rules.

## Reliability
- Health endpoint at `/health`.
- Add structured logging.
- Add metrics for chat latency, provider lookup latency, booking latency, and error rates.
- Provide a retry envelope around external compute APIs.
- Use Cloud Run revisioning and staging/prod separation.

## Service contract
The API must return structured objects:

```
{
  "text": "Human readable answer",
  "action": "NAVIGATE_TO | CHAT_PROVIDER | BOOK_PROVIDER | BOOK_RIDE",
  "target": "screen-or-service-name",
  "data": { }
}
```

## Avoid
- Public Firebase keys in the repo.
- Hardcoded demo secrets.
- Open Firestore writes.
- Chat service that accepts unauthenticated requests.
- Provider registration that bypasses verification.
