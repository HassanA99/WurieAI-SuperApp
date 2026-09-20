"""Regression gate for the agent runtime (Step 1).

These tests exist because the LangGraph path previously failed at import time and the
API silently served deterministic router answers instead, so nothing ever surfaced the
breakage. They assert that the runtime imports, that the graph compiles, and that the
API prefers the agent while marking degraded responses explicitly.
"""

import asyncio
import os
import unittest
from unittest import mock

from app import main


class AgentRuntimeTests(unittest.TestCase):
    def test_orchestrator_imports_and_exposes_contract(self):
        from app.agents.orchestrator import (
            AgentError,
            agent_status,
            app_graph,
            model_name,
            run_orchestrator,
        )

        self.assertTrue(callable(run_orchestrator))
        self.assertTrue(issubclass(AgentError, RuntimeError))
        self.assertIsNotNone(app_graph)
        self.assertTrue(model_name())

        status = agent_status()
        self.assertIn("available", status)
        self.assertIn("model", status)
        self.assertEqual(status.get("graph"), "compiled")

    def test_graph_compiles_with_supervisor_and_specialist_nodes(self):
        from app.agents.orchestrator import app_graph

        graph = app_graph.get_graph() if hasattr(app_graph, "get_graph") else None
        if graph is None:  # pragma: no cover - depends on the langgraph version
            self.skipTest("graph introspection unavailable in this langgraph version")

        nodes = set(graph.nodes)
        for expected in {"router", "market", "artisan", "wallet", "general"}:
            self.assertIn(expected, nodes, f"graph is missing node {expected!r}")

    def test_orchestrator_raises_agent_error_without_credentials(self):
        from app.agents.orchestrator import AgentError, run_orchestrator

        with mock.patch.dict(os.environ, {"GEMINI_API_KEY": "", "GOOGLE_API_KEY": ""}):
            with self.assertRaises(AgentError) as context:
                run_orchestrator("What is the price of rice?", "user-1")

        self.assertIn("GEMINI_API_KEY", str(context.exception))

    def test_chat_prefers_the_agent_runtime(self):
        stub = lambda message, user_id: {  # noqa: E731 - simple test double
            "text": "Agent answer",
            "action": "NAVIGATE_TO",
            "target": "Market Prices",
            "data": {"commodity": "rice"},
            "service": "market",
            "workflow_id": "wf-test",
        }

        with mock.patch.object(main, "run_orchestrator", stub):
            response = asyncio.run(
                main.chat_endpoint(
                    main.ChatRequest(message="rice price"),
                    {"uid": "user-1", "claims": {}},
                )
            )

        self.assertEqual(response.text, "Agent answer")
        self.assertEqual(response.workflow_id, "wf-test")
        self.assertNotIn("degraded", response.data)

    def test_chat_marks_degraded_response_when_agent_fails(self):
        def failing_agent(message, user_id):
            raise main.AgentError("model quota exceeded")

        with mock.patch.object(main, "run_orchestrator", failing_agent):
            response = asyncio.run(
                main.chat_endpoint(
                    main.ChatRequest(message="What is the price of rice in Freetown?"),
                    {"uid": "user-1", "claims": {}},
                )
            )

        self.assertTrue(response.data.get("degraded"))
        self.assertIn("model quota exceeded", response.data.get("degraded_reason", ""))
        self.assertEqual(response.service, "market")

    def test_chat_marks_degraded_response_when_agent_is_unavailable(self):
        with mock.patch.object(main, "run_orchestrator", None):
            response = asyncio.run(
                main.chat_endpoint(
                    main.ChatRequest(message="What is my wallet balance?"),
                    {"uid": "user-1", "claims": {}},
                )
            )

        self.assertTrue(response.data.get("degraded"))
        self.assertEqual(response.service, "wallet")

    def test_health_stays_flat_and_string_valued(self):
        """The Android client declares /health as Map<String, String>."""
        payload = main.health_check()

        self.assertEqual(payload["status"], "healthy")
        for key in (
            "agent_available",
            "agent_model",
            "agent_fallback",
            "agent_reason",
            "firebase_admin",
            "storage",
            "error_tracking",
            "auth_mode",
            "app_check",
        ):
            self.assertIn(key, payload)
            self.assertIsInstance(payload[key], str, f"{key} must stay a string")

    def test_runtime_status_endpoint_reports_structured_status(self):
        status = main.runtime_status_endpoint({"uid": "user-1", "claims": {}})

        self.assertIn("available", status["agent"])
        self.assertIn("fallback", status["agent"])
        self.assertIn(status["storage"]["storage"], {"firestore", "memory"})
        self.assertIn(status["error_tracking"], {"sentry", "off"})


if __name__ == "__main__":
    unittest.main()
