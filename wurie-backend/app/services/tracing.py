"""Optional LangSmith tracing boundaries for WurieAI application flows."""

from __future__ import annotations

from typing import Any, Callable, TypeVar

from langsmith import traceable

T = TypeVar("T")


def trace_chat_flow(handler: Callable[..., T]) -> Callable[..., T]:
    """Name the request boundary while preserving local no-key operation."""
    return traceable(
        name="wurieai.chat",
        run_type="chain",
        process_inputs=lambda inputs: {
            "message": inputs.get("message", ""),
            "user_id": inputs.get("user_id", "unknown"),
        },
    )(handler)


def trace_agent_flow(handler: Callable[..., T]) -> Callable[..., T]:
    """Name the LangGraph orchestration boundary for agent diagnostics."""
    return traceable(name="wurieai.agent_orchestrator", run_type="chain")(handler)
