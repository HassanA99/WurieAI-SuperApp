"""Provider service contract and demo provider matching layer."""

from __future__ import annotations


def find_provider(query: str) -> dict:
    """Return a structured provider recommendation.

    Production direction:
    - Use Firestore or a vector database to match provider profiles to user intent.
    - Return ranked provider records with trade, location, rating, and verification.
    """
    text = query.lower()
    if "plumber" in text or "pipe" in text or "leak" in text:
        return {
            "text": "I found a great plumber nearby. Abu has a 4.8 star rating and can fix your leaking pipe. Would you like to chat with him?",
            "action": "NAVIGATE_TO",
            "target": "Hire Artisan:Plumber",
            "data": {
                "provider_name": "Abu",
                "profession": "Plumber",
                "rating": 4.8,
                "location": "nearby",
                "trade": "Plumber",
                "verified": True,
            },
        }

    return {
        "text": "I can help you find a skilled artisan. What kind of work do you need done?",
        "action": "NAVIGATE_TO",
        "target": "Hire Artisan",
        "data": {},
    }
