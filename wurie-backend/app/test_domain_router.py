import asyncio
import unittest

from fastapi import HTTPException

from app.services.auth import verify_firebase_token
from app.services.domain_router import DomainRouter
from app.services.profile_service import ProfileService
from app.services.service_contracts import NotificationItem, ProfileSettings


class DomainRouterContractTests(unittest.TestCase):
    def test_market_route_returns_structured_agent_response(self):
        router = DomainRouter()
        response = router.route("What is the price of rice in Freetown?")

        self.assertEqual(response.service.value, "market")
        self.assertEqual(response.action, "NAVIGATE_TO")
        self.assertEqual(response.target, "Market Prices")
        self.assertIn("rice", response.text.lower())

    def test_provider_route_returns_structured_agent_response(self):
        router = DomainRouter()
        response = router.route("I need a plumber for a leaking pipe")

        self.assertEqual(response.service.value, "provider")
        self.assertEqual(response.action, "NAVIGATE_TO")
        self.assertEqual(response.target, "Hire Artisan:Plumber")
        self.assertIn("plumber", response.text.lower())

    def test_wallet_route_returns_structured_agent_response(self):
        router = DomainRouter()
        response = router.route("What is my wallet balance?")

        self.assertEqual(response.service.value, "wallet")
        self.assertEqual(response.action, "SHOW_WALLET")
        self.assertEqual(response.target, "Wallet")
        self.assertEqual(response.data["balance"], 150.0)

    def test_missing_authorization_header_is_rejected(self):
        with self.assertRaises(HTTPException) as context:
            asyncio.run(verify_firebase_token(None))

        self.assertEqual(context.exception.status_code, 401)

    def test_profile_settings_and_notifications_contract_shape(self):
        settings = ProfileSettings(
            notifications_enabled=True,
            biometric_enabled=True,
            push_enabled=True,
            offline_cache_enabled=True,
            language="en",
        )
        notification = NotificationItem(
            id="n-1",
            title="New provider match",
            body="A verified plumber is available in your area.",
            read=False,
            category="provider",
        )

        self.assertTrue(settings.notifications_enabled)
        self.assertTrue(settings.biometric_enabled)
        self.assertEqual(notification.category, "provider")
        self.assertEqual(notification.title, "New provider match")

    def test_profile_updates_are_scoped_to_uid(self):
        service = ProfileService()
        profile = service.update_profile(
            "user-123",
            {"full_name": "Aminata Conteh", "phone": "+23270000000", "city": "Freetown"},
            "aminata@example.com",
        )

        self.assertEqual(profile.uid, "user-123")
        self.assertEqual(profile.full_name, "Aminata Conteh")
        self.assertTrue(profile.profile_completed)
        self.assertEqual(service.get_profile("other-user").uid, "other-user")
        self.assertNotEqual(service.get_profile("other-user").full_name, profile.full_name)


if __name__ == "__main__":
    unittest.main()
