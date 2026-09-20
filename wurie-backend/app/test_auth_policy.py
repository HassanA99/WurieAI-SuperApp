"""Regression gate for the authentication policy (Step 3).

The previous implementation returned an identity for the literal string ``guest-token``
(or ``dev_user``) before any verification, and the development flag accepted *any* token.
Both are authentication bypasses that would have gone live with the next deployment, so
these tests pin the policy:

* no token value is trusted without Firebase verification,
* the development fallback is refused on a production runtime,
* App Check is enforced only when it is explicitly switched on, and fails closed.
"""

import contextlib
import os
import unittest
from unittest import mock

from fastapi import HTTPException

from app.services import auth as auth_service

try:  # pragma: no cover - depends on the installed test extras
    from fastapi.testclient import TestClient

    import httpx  # noqa: F401  (imported to confirm TestClient can run)

    HTTP_AVAILABLE = True
except Exception:  # pragma: no cover
    HTTP_AVAILABLE = False

# Every environment variable the policy reads. Tests blank them all so the result does
# not depend on the machine, then set only what the case is about.
AUTH_ENV_KEYS = (
    "WURIE_DEV_AUTH_FALLBACK",
    "WURIE_APP_CHECK_ENFORCE",
    "WURIE_ENV",
    "SENTRY_ENVIRONMENT",
    "RENDER",
    "K_SERVICE",
    "WEBSITE_INSTANCE_ID",
    "FIREBASE_SERVICE_ACCOUNT_JSON",
    "FIREBASE_SERVICE_ACCOUNT_PATH",
    "GOOGLE_APPLICATION_CREDENTIALS",
)


@contextlib.contextmanager
def auth_env(**values):
    clean = {key: "" for key in AUTH_ENV_KEYS}
    clean.update(values)
    with mock.patch.dict(os.environ, clean):
        yield


class UnauthenticatedRequestTests(unittest.IsolatedAsyncioTestCase):
    """Nothing is authenticated without a verifiable credential."""

    async def test_missing_header_is_rejected(self):
        with auth_env():
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token(None, None)
        self.assertEqual(context.exception.status_code, 401)

    async def test_guest_token_is_not_a_credential(self):
        """The exact bypass that this step removes."""
        with auth_env():
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token("Bearer guest-token", None)
        self.assertEqual(context.exception.status_code, 401)

    async def test_dev_user_token_is_not_a_credential(self):
        with auth_env():
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token("Bearer dev_user", None)
        self.assertEqual(context.exception.status_code, 401)

    async def test_bogus_token_is_rejected(self):
        with auth_env():
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token("Bearer not-a-real-token", None)
        self.assertEqual(context.exception.status_code, 401)

    async def test_wrong_scheme_is_rejected(self):
        with auth_env():
            with self.assertRaises(HTTPException):
                await auth_service.verify_firebase_token("Basic abc123", None)

    async def test_empty_bearer_token_is_rejected(self):
        with auth_env():
            with self.assertRaises(HTTPException):
                await auth_service.verify_firebase_token("Bearer   ", None)


class DevelopmentFallbackTests(unittest.IsolatedAsyncioTestCase):
    """The dev fallback works locally and is impossible to enable in production."""

    async def test_fallback_is_off_by_default(self):
        with auth_env():
            self.assertFalse(auth_service.dev_fallback_requested())

    async def test_fallback_serves_a_local_developer(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true"):
            user = await auth_service.verify_firebase_token(None, None)
        self.assertEqual(user["uid"], "dev_user")
        self.assertTrue(user["dev"])

    async def test_fallback_is_refused_on_render(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true", RENDER="true"):
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token(None, None)
        self.assertEqual(context.exception.status_code, 401)

    async def test_fallback_is_refused_when_environment_is_production(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true", SENTRY_ENVIRONMENT="production"):
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token(None, None)
        self.assertEqual(context.exception.status_code, 401)

    async def test_production_refuses_even_a_flagged_bogus_token(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true", RENDER="true"):
            with self.assertRaises(HTTPException):
                await auth_service.verify_firebase_token("Bearer guest-token", None)

    def test_is_production_recognises_the_markers(self):
        with auth_env():
            self.assertFalse(auth_service.is_production())
        with auth_env(SENTRY_ENVIRONMENT="production"):
            self.assertTrue(auth_service.is_production())
        with auth_env(K_SERVICE="wurie"):
            self.assertTrue(auth_service.is_production())


class AppCheckTests(unittest.IsolatedAsyncioTestCase):
    """App Check fails closed, and only when it is switched on."""

    async def test_enforcement_requires_a_token(self):
        with auth_env(WURIE_APP_CHECK_ENFORCE="true", WURIE_DEV_AUTH_FALLBACK="true"):
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token(None, None)
        self.assertEqual(context.exception.status_code, 401)
        self.assertIn("App Check", context.exception.detail)

    async def test_enforcement_rejects_an_invalid_token(self):
        with auth_env(WURIE_APP_CHECK_ENFORCE="true", WURIE_DEV_AUTH_FALLBACK="true"):
            with self.assertRaises(HTTPException) as context:
                await auth_service.verify_firebase_token(None, "not-a-real-app-check-token")
        self.assertEqual(context.exception.status_code, 401)

    async def test_missing_token_is_ignored_when_not_enforced(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true"):
            user = await auth_service.verify_firebase_token(None, None)
        self.assertEqual(user["uid"], "dev_user")


class AuthHealthTests(unittest.TestCase):
    """The auth posture is externally visible, so a misconfiguration cannot hide."""

    def test_default_posture_is_firebase(self):
        with auth_env():
            health = auth_service.auth_health()
        self.assertEqual(health["mode"], "firebase")
        self.assertEqual(health["app_check"], "off")

    def test_local_fallback_is_reported(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true"):
            health = auth_service.auth_health()
        self.assertEqual(health["mode"], "dev_fallback")

    def test_production_refusal_is_reported(self):
        with auth_env(WURIE_DEV_AUTH_FALLBACK="true", RENDER="true"):
            health = auth_service.auth_health()
        self.assertEqual(health["mode"], "refused_dev_fallback")

    def test_app_check_enforcement_is_reported(self):
        with auth_env(WURIE_APP_CHECK_ENFORCE="true"):
            health = auth_service.auth_health()
        self.assertEqual(health["app_check"], "enforced")

    def test_health_endpoint_exposes_the_auth_posture(self):
        from app import main

        with auth_env(WURIE_DEV_AUTH_FALLBACK="true", RENDER="true"):
            payload = main.health_check()

        self.assertEqual(payload["auth_mode"], "refused_dev_fallback")
        self.assertEqual(payload["app_check"], "off")
        self.assertIsInstance(payload["auth_mode"], str)
        self.assertIsInstance(payload["app_check"], str)


@unittest.skipUnless(HTTP_AVAILABLE, "HTTP client extras are not installed")
class HttpAuthTests(unittest.TestCase):
    """End-to-end proof through the ASGI app, not just the dependency function."""

    def setUp(self):
        from app import main

        self.client = TestClient(main.app)

    def test_health_stays_public(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "healthy")

    def test_guest_token_is_rejected_over_http(self):
        with auth_env():
            response = self.client.get("/api/v1/profile", headers={"Authorization": "Bearer guest-token"})
        self.assertEqual(response.status_code, 401)

    def test_missing_token_is_rejected_over_http(self):
        with auth_env():
            response = self.client.get("/api/v1/wallet/balance")
        self.assertEqual(response.status_code, 401)


if __name__ == "__main__":
    unittest.main()
