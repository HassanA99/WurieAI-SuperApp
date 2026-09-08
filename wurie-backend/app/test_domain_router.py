import unittest

from app.services.domain_router import DomainRouter


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


if __name__ == "__main__":
    unittest.main()
