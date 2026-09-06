def check_market_price(query: str) -> dict:
    """
    The Market Agent. 
    In production, this queries your PostgreSQL/Firestore database for real-time prices.
    """
    # Simulated Database Query (Local RAG)
    market_db = {
        "rice": {"price": "$25", "location": "Lumley Market"},
        "cement": {"price": "$8", "location": "Waterloo"},
        "fish": {"price": "$5", "location": "Aberdeen"}
    }
    
    query = query.lower()
    
    for item, data in market_db.items():
        if item in query:
            return {
                "text": f"The current price of {item} is {data['price']} at {data['location']}.",
                "action": "NAVIGATE_TO",
                "target": "Market Prices"
            }
            
    return {
        "text": "I can check market prices for you. What item are you looking for?",
        "action": "NAVIGATE_TO",
        "target": "Market Prices"
    }
