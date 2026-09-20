"""Tests for the deployment drift checker (Step 2).

These cover the comparison logic offline; the live comparison is run with
``uv run python -m app.deployment_check <base-url>``.
"""

import unittest
from unittest import mock

from app.deployment_check import (
    REQUIRED_CHAT_ALIASES,
    REQUIRED_HEALTH_FIELDS,
    compare_health,
    compare_routes,
    expected_routes,
)


class DeploymentContractTests(unittest.TestCase):
    def test_expected_routes_include_mobile_chat_aliases(self):
        routes = set(expected_routes())

        for alias in REQUIRED_CHAT_ALIASES:
            self.assertIn(alias, routes, f"code no longer exposes {alias}")
        self.assertIn("/health", routes)

    def test_compare_routes_flags_missing_and_unexpected_paths(self):
        result = compare_routes(["/health", "/chat"], ["/health", "/api/v1/legacy"])

        self.assertFalse(result["in_sync"])
        self.assertEqual(result["missing"], ["/chat"])
        self.assertEqual(result["unexpected"], ["/api/v1/legacy"])
        self.assertEqual(result["missing_chat_aliases"], ["/chat"])

    def test_compare_routes_reports_in_sync(self):
        result = compare_routes(["/health", "/chat"], ["/chat", "/health"])

        self.assertTrue(result["in_sync"])
        self.assertEqual(result["missing_chat_aliases"], [])

    def test_compare_health_requires_the_flat_string_contract(self):
        healthy = {field: "value" for field in REQUIRED_HEALTH_FIELDS}
        self.assertTrue(compare_health(healthy)["in_sync"])

    def test_compare_health_flags_a_legacy_payload(self):
        result = compare_health({"status": "healthy"})

        self.assertFalse(result["in_sync"])
        self.assertIn("agent_available", result["missing"])
        self.assertIn("firebase_admin", result["missing"])

    def test_compare_health_flags_non_string_values(self):
        payload = {field: "value" for field in REQUIRED_HEALTH_FIELDS}
        payload["agent_available"] = True

        result = compare_health(payload)

        self.assertFalse(result["in_sync"])
        self.assertEqual(result["wrong_type"], ["agent_available"])

    def test_compare_health_handles_non_object_payload(self):
        self.assertFalse(compare_health([1, 2, 3])["in_sync"])
        self.assertFalse(compare_health(None)["in_sync"])

    def test_check_reports_in_sync_when_the_deployment_matches_the_code(self):
        from app import deployment_check

        live_openapi = {
            "paths": {path: {"get": {}} for path in deployment_check.expected_routes()}
        }
        live_health = {field: "value" for field in REQUIRED_HEALTH_FIELDS}

        def fake_fetch(url, timeout=30.0):
            return live_openapi if url.endswith("/openapi.json") else live_health

        with mock.patch.object(deployment_check, "fetch_json", fake_fetch):
            report = deployment_check.check("https://example.test")

        self.assertTrue(report["in_sync"])
        self.assertEqual(report["routes"]["missing"], [])
        self.assertEqual(report["health"]["missing"], [])


if __name__ == "__main__":
    unittest.main()
