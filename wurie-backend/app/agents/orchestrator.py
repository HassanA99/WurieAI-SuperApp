"""LangGraph agent runtime for WurieAI.

This module owns the model-backed orchestration path (supervisor → specialist node).

It fails loudly by design: missing credentials, missing dependencies and model
errors raise :class:`AgentError` so the API layer can log them, expose them on
``/health`` and mark degraded responses explicitly, instead of silently answering
from a different code path.
"""

from __future__ import annotations

import logging
import os
from typing import Sequence, TypedDict
from uuid import uuid4

from langchain_core.messages import BaseMessage, HumanMessage
from langchain_google_genai import ChatGoogleGenerativeAI
from langgraph.graph import END, StateGraph

from app.agents.artisan_agent import find_artisan
from app.agents.market_agent import check_market_price
from app.services.tracing import trace_agent_flow

logger = logging.getLogger("wurieai.agents")

DEFAULT_MODEL = "gemini-2.5-flash"
VALID_INTENTS = ("MARKET", "ARTISAN", "WALLET", "GENERAL")


class AgentError(RuntimeError):
    """Raised when the agent runtime cannot produce a usable response."""


# --- Configuration -------------------------------------------------------------


def model_name() -> str:
    """Return the configured Gemini model name."""
    return (os.getenv("GEMINI_MODEL") or DEFAULT_MODEL).strip() or DEFAULT_MODEL


def _api_key() -> str | None:
    """Return the server-side Gemini key (GEMINI_API_KEY wins over GOOGLE_API_KEY)."""
    return os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY") or None


def agent_status() -> dict:
    """Describe agent readiness for health checks and ops dashboards."""
    key_configured = bool(_api_key())
    return {
        "available": key_configured,
        "model": model_name(),
        "graph": "compiled",
        "reason": None if key_configured else "missing_credentials: set GEMINI_API_KEY",
    }


def _get_llm(temperature: float = 0.0) -> ChatGoogleGenerativeAI:
    """Build the Gemini chat model, failing loudly when it is not configured."""
    api_key = _api_key()
    if not api_key:
        raise AgentError("GEMINI_API_KEY (or GOOGLE_API_KEY) is not configured")

    # langchain-google-genai reads the credential from the environment; normalise
    # the backend's GEMINI_API_KEY convention onto the client it expects.
    os.environ.setdefault("GOOGLE_API_KEY", api_key)
    try:
        return ChatGoogleGenerativeAI(model=model_name(), temperature=temperature)
    except Exception as exc:  # pragma: no cover - depends on installed client version
        raise AgentError(f"could not initialise model {model_name()!r}: {exc}") from exc


def _message_text(content: object) -> str:
    """Normalise message content, which may be a string or multimodal content blocks."""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for part in content:
            if isinstance(part, str):
                parts.append(part)
            elif isinstance(part, dict) and isinstance(part.get("text"), str):
                parts.append(part["text"])
        return " ".join(parts)
    return str(content or "")


# --- Graph state ---------------------------------------------------------------


class AgentState(TypedDict):
    """State carried through the orchestration graph."""

    messages: Sequence[BaseMessage]
    intent: str
    final_response: dict
    user_id: str


# --- Nodes ---------------------------------------------------------------------


def get_intent(state: AgentState) -> dict:
    """Router node: classify the user's request into a single domain."""
    llm = _get_llm(temperature=0.0)
    last_message = _message_text(state["messages"][-1].content)

    prompt = (
        "Classify the user's request into exactly one category.\n"
        "Categories:\n"
        "- MARKET: prices of goods, commodities or produce\n"
        "- ARTISAN: tradespeople and service providers (plumbers, electricians, mechanics)\n"
        "- WALLET: balances, payments, transactions\n"
        "- GENERAL: anything else\n"
        "Reply with the category name only.\n\n"
        f"User request: {last_message}"
    )

    response = llm.invoke(prompt)
    candidate = _message_text(response.content).strip().upper()

    for intent in VALID_INTENTS:
        if intent in candidate:
            logger.info("intent_classified intent=%s", intent)
            return {"intent": intent}

    logger.warning("intent_unparsed raw=%r defaulting=GENERAL", candidate[:120])
    return {"intent": "GENERAL"}


def market_node(state: AgentState) -> dict:
    """Market specialist node.

    Placeholder tool: this will call the single market domain service instead of a
    duplicated lookup table in a later step.
    """
    last_message = _message_text(state["messages"][-1].content)
    response = check_market_price(last_message)
    return {"final_response": {**response, "service": "market"}}


def artisan_node(state: AgentState) -> dict:
    """Provider/artisan matchmaking node (placeholder tool, see market_node)."""
    last_message = _message_text(state["messages"][-1].content)
    response = find_artisan(last_message)
    return {"final_response": {**response, "service": "provider"}}


def wallet_node(state: AgentState) -> dict:
    """Wallet node: deterministic, will read the real ledger in a later step."""
    response = {
        "text": "Your current wallet balance is $150.00.",
        "action": "SHOW_WALLET",
        "target": "Wallet",
        "service": "wallet",
    }
    return {"final_response": response}


def general_node(state: AgentState) -> dict:
    """General conversation node."""
    llm = _get_llm(temperature=0.7)

    system_prompt = (
        "You are WurieAI, a practical local-services and commerce assistant. "
        "Answer in the language the user writes in, and be concise and concrete."
    )
    messages = [HumanMessage(content=system_prompt)] + list(state["messages"])
    ai_message = llm.invoke(messages)

    response = {
        "text": _message_text(ai_message.content),
        "action": None,
        "target": None,
        "service": "general",
    }
    return {"final_response": response}


# --- Routing -------------------------------------------------------------------


def route_intent(state: AgentState) -> str:
    """Conditional edge: map the classified intent onto a graph node."""
    intent = state.get("intent", "GENERAL")
    if intent == "MARKET":
        return "market"
    if intent == "ARTISAN":
        return "artisan"
    if intent == "WALLET":
        return "wallet"
    return "general"


def build_graph():
    """Compile the supervisor/specialist graph."""
    workflow = StateGraph(AgentState)

    workflow.add_node("router", get_intent)
    workflow.add_node("market", market_node)
    workflow.add_node("artisan", artisan_node)
    workflow.add_node("wallet", wallet_node)
    workflow.add_node("general", general_node)

    workflow.set_entry_point("router")
    workflow.add_conditional_edges(
        "router",
        route_intent,
        {
            "market": "market",
            "artisan": "artisan",
            "wallet": "wallet",
            "general": "general",
        },
    )

    workflow.add_edge("market", END)
    workflow.add_edge("artisan", END)
    workflow.add_edge("wallet", END)
    workflow.add_edge("general", END)

    return workflow.compile()


# Compiled once at import; importing this module never requires model credentials.
app_graph = build_graph()
logger.info("agent_graph_compiled model=%s", model_name())


# --- Entry point ---------------------------------------------------------------


@trace_agent_flow
def run_orchestrator(message: str, user_id: str) -> dict:
    """Execute the orchestration graph for a single user message.

    Returns the canonical response contract ``{text, action, target, data, service,
    workflow_id}``. Raises :class:`AgentError` when no usable response can be produced.
    """
    if not message or not message.strip():
        raise AgentError("empty message")

    workflow_id = f"wf-{uuid4().hex[:12]}"
    initial_state: AgentState = {
        "messages": [HumanMessage(content=message)],
        "intent": "",
        "final_response": {},
        "user_id": user_id,
    }

    try:
        result = app_graph.invoke(initial_state)
    except AgentError:
        raise
    except Exception as exc:
        logger.exception("agent_graph_failed workflow_id=%s user_id=%s", workflow_id, user_id)
        raise AgentError(str(exc)) from exc

    final = result.get("final_response") or {}
    text = (final.get("text") or "").strip()
    if not text:
        logger.error(
            "agent_empty_response workflow_id=%s intent=%s",
            workflow_id,
            result.get("intent"),
        )
        raise AgentError("agent produced no response text")

    logger.info(
        "agent_responded workflow_id=%s user_id=%s intent=%s",
        workflow_id,
        user_id,
        result.get("intent"),
    )
    return {
        "text": text,
        "action": final.get("action"),
        "target": final.get("target"),
        "data": final.get("data") or {},
        "service": final.get("service"),
        "workflow_id": workflow_id,
    }
