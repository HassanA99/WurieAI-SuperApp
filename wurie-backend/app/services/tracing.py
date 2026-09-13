"""Optional LangSmith tracing boundaries for WurieAI application flows."""

from __future__ import annotations

from typing import Any, Callable, TypeVar

T = TypeVar("T")

def _traceable(**kwargs: Any) -> Callable[[Callable[..., T]], Callable[..., T]]:
    try:
        from langsmith import traceable
    except (ImportError, TypeError):
        return lambda handler: handler
    return traceable(**kwargs)


def trace_chat_flow(handler: Callable[..., T]) -> Callable[..., T]:
    """Name the request boundary while preserving local no-key operation."""
    return _traceable(
        name="wurieai.chat",
        run_type="chain",
        process_inputs=lambda inputs: {
            "message": inputs.get("message", ""),
            "user_id": inputs.get("user_id", "unknown"),
        },
    )(handler)


def trace_agent_flow(handler: Callable[..., T]) -> Callable[..., T]:
    """Name the LangGraph orchestration boundary for agent diagnostics."""
    return _traceable(name="wurieai.agent_orchestrator", run_type="chain")(handler)
