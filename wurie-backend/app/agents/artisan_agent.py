def find_artisan(query: str) -> dict:
    """
    The Artisan Matchmaking Agent.
    In production, this uses Vector Embeddings to match the query against artisan profiles.
    """
    # Simulated Vector Match
    if "plumber" in query.lower() or "pipe" in query.lower() or "leak" in query.lower():
        return {
            "text": "I found a great plumber nearby. Abu has a 4.8 star rating and can fix your leaking pipe. Would you like to chat with him?",
            "action": "NAVIGATE_TO",
            "target": "Hire Artisan:Plumber"
        }
        
    return {
        "text": "I can help you find a skilled artisan. What kind of work do you need done?",
        "action": "NAVIGATE_TO",
        "target": "Hire Artisan"
    }
