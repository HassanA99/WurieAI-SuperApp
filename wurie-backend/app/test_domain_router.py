import asyncio
import unittest

from fastapi import HTTPException

from app.services.auth import verify_firebase_token
from app.services.domain_router import DomainRouter
from app.services.profile_service import ProfileService
from app.services.service_contracts import CommentItem, NotificationItem, ProfileSettings, ProviderProfile
from app.services.social_service import SocialService


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

    def test_social_comments_and_notifications_behave_as_scoped_feed_state(self):
        social = SocialService()

        created_comment = social.add_comment("user-123", "This is a great community post.", username="Aminata")
        notification = social.add_notification(
            "user-123",
            "New provider match",
            "A verified plumber is available near you.",
            category="provider",
        )

        self.assertIsInstance(created_comment, CommentItem)
        self.assertEqual(created_comment.username, "Aminata")
        self.assertIn("community", created_comment.body.lower())

        self.assertEqual(notification.title, "New provider match")
        self.assertFalse(notification.read)
        self.assertEqual(len(social.list_notifications("user-123")), 1)

        read_notification = social.mark_notification_read("user-123", notification.id)
        self.assertTrue(read_notification.read)

        self.assertEqual(len(social.list_comments("user-123")), 1)
        self.assertEqual(len(social.list_comments("other-user")), 0)

    def test_provider_registration_captures_pending_verification_and_admin_approval(self):
        from app.services.provider_service import ProviderService

        service = ProviderService()
        submitted = service.register_provider({
            "name": "Abu Koroma",
            "profession": "Mechanic",
            "city": "Freetown",
            "experience": "4 years",
        })

        self.assertEqual(submitted["provider"]["verificationStatus"], "pending")

        approved = service.approve_provider("provider-001")
        self.assertEqual(approved["verificationStatus"], "approved")

        profile = ProviderProfile(
            provider_id="provider-001",
            name="Abu Koroma",
            profession="Mechanic",
            city="Freetown",
            experience="4 years",
            verification_status="approved",
        )
        self.assertEqual(profile.verification_status, "approved")

    def test_provider_service_exposes_pending_list_for_admin_dashboard(self):
        from app.services.provider_service import ProviderService

        service = ProviderService()
        service.register_provider({
            "providerId": "provider-900",
            "name": "Mariama Bangura",
            "profession": "Plumber",
            "city": "Bo",
            "experience": "3 years",
        })

        pending = service.list_pending_providers()
        self.assertTrue(any(provider["providerId"] == "provider-900" for provider in pending))
        self.assertTrue(any(provider["verificationStatus"] == "pending" for provider in pending))

    def test_profile_updates_sanitize_input_and_reject_invalid_settings(self):
        service = ProfileService()

        with self.assertRaises(ValueError):
            service.update_profile(
                "user-privacy-check",
                {"full_name": "   ", "phone": "invalid", "city": "Freetown"},
                "user@example.com",
            )

        updated = service.update_profile(
            "user-privacy-check",
            {"full_name": "Aminata Conteh", "phone": "+23270000000", "city": "Freetown"},
            "user@example.com",
        )
        self.assertEqual(updated.full_name, "Aminata Conteh")
        self.assertEqual(updated.phone, "+23270000000")

        with self.assertRaises(ValueError):
            service.update_settings("user-privacy-check", {"language": "francais"})

        settings = service.update_settings(
            "user-privacy-check",
            {"notifications_enabled": False, "language": "en"},
        )
        self.assertFalse(settings.notifications_enabled)
        self.assertEqual(settings.language, "en")

    def test_social_like_state_tracks_real_engagement(self):
        social = SocialService()
        comment = social.add_comment("user-social", "This is a strong community post.", username="Aminata")

        liked = social.toggle_like("user-social", comment.id)
        self.assertTrue(liked.liked)
        self.assertEqual(liked.likes_count, 1)
        self.assertTrue(social.is_liked("user-social", comment.id))

        toggled_off = social.toggle_like("user-social", comment.id)
        self.assertFalse(toggled_off.liked)
        self.assertEqual(toggled_off.likes_count, 0)


if __name__ == "__main__":
    unittest.main()
