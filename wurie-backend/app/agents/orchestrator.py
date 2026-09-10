import json
from typing import TypedDict, Annotated, Sequence
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langgraph.graph import StateGraph, END
from app.agents.market_agent import check_market_price
from app.agents.artisan_agent import find_artisan
from app.services.tracing import trace_agent_flow

# Define the State for LangGraph
class AgentState(TypedDict):
    messages: Sequence[BaseMessage]
    intent: str
    final_response: dict
    user_id: str

def get_intent(state: AgentState):
    """Router Node: Determines the user's intent."""
    llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0.0)
    messages = state["messages"]
    last_message = messages[-1].content
    
    prompt = f"""
    Analyze the user's message and determine their intent.
    Choose exactly one: MARKET, ARTISAN, WALLET, or GENERAL.
    
    User Message: "{last_message}"
    
    Return ONLY the category name.
    """
    
    # In production, use structured output. For now, simple text extraction.
    response = llm.invoke(prompt)
    intent = response.content.strip().upper()
    
    # Clean up any potential markdown or extra text
    for valid_intent in ["MARKET", "ARTISAN", "WALLET", "GENERAL"]:
        if valid_intent in intent:
            return {"intent": valid_intent}
            
    return {"intent": "GENERAL"}

def market_node(state: AgentState):
    """Market Specialist Node"""
    last_message = state["messages"][-1].content
    # In a full setup, this connects to Cloud SQL/Firestore (PostgreSQL)
    response = check_market_price(last_message)
    return {"final_response": response}

def artisan_node(state: AgentState):
    """Artisan Matchmaking Node"""
    last_message = state["messages"][-1].content
    # In a full setup, this uses Vector Database for semantic search
    response = find_artisan(last_message)
    return {"final_response": response}

def wallet_node(state: AgentState):
    """Financial Sandbox Node"""
    # Strictly deterministic, accesses Cloud SQL (PostgreSQL) ledger
    response = {
        "text": "Your current wallet balance is $150.00.",
        "action": "NAVIGATE_TO",
        "target": "Wallet"
    }
    return {"final_response": response}

def general_node(state: AgentState):
    """General Conversation Node"""
    llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0.7)
    
    sys_prompt = "You are WurieAI, an intelligent agent for the MRU region. Be helpful and concise."
    messages = [HumanMessage(content=sys_prompt)] + list(state["messages"])
    
    ai_msg = llm.invoke(messages)
    
    response = {
        "text": ai_msg.content,
        "action": None,
        "target": None
    }
    return {"final_response": response}

# Define the Routing Logic
def route_intent(state: AgentState):
    intent = state.get("intent", "GENERAL")
    if intent == "MARKET":
        return "market"
    elif intent == "ARTISAN":
        return "artisan"
    elif intent == "WALLET":
        return "wallet"
    else:
        return "general"

# Build the LangGraph
def build_graph():
    workflow = StateGraph(AgentState)
    
    # Add Nodes
    workflow.add_node("router", get_intent)
    workflow.add_node("market", market_node)
    workflow.add_node("artisan", artisan_node)
    workflow.add_node("wallet", wallet_node)
    workflow.add_node("general", general_node)
    
    # Add Edges
    workflow.set_entry_point("router")
    
    workflow.add_conditional_edges(
        "router",
        route_intent,
        {
            "market": "market",
            "artisan": "artisan",
            "wallet": "wallet",
            "general": "general"
        }
    )
    
    # All specialist nodes go to END
    workflow.add_edge("market", END)
    workflow.add_edge("artisan", END)
    workflow.add_edge("wallet", END)
    workflow.add_edge("general", END)
    
    return workflow.compile()

# Compile the graph once
app_graph = build_graph()

@trace_agent_flow
def run_orchestrator(message: str, user_id: str) -> dict:
    """
    Executes the full LangGraph multi-agent flow.
    """
    try:
        initial_state = {
            "messages": [HumanMessage(content=message)],
            "user_id": user_id
        }
        
        # Run the graph
        result = app_graph.invoke(initial_state)
        
        return result.get("final_response", {"text": "Agent routing failed."})
            
    except Exception as e:
        return {"text": f"LangGraph Error: {str(e)}"}
