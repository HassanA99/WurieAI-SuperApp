from typing import Dict, Any

class MarketService:
    """Production-oriented market price service skeleton."""

    def __init__(self, datastore=None):
        self.datastore = datastore

    def get_price(self, commodity: str, location: str = "") -> Dict[str, Any]:
        """Return a price record. Connect this to Firestore/Cloud SQL in production."""
        market_db = {
            "rice": {"price": "$25", "location": "Lumley Market", "unit": "bag"},
            "cement": {"price": "$8", "location": "Waterloo", "unit": "bag"},
            "fish": {"price": "$5", "location": "Aberdeen", "unit": "kg"},
        }

        commodity_key = commodity.lower()
        if commodity_key in market_db:
            item = market_db[commodity_key]
            return {
                "commodity": commodity_key,
                "price": item["price"],
                "location": item["location"],
                "unit": item["unit"],
                "source": "demo-market-feed",
                "updatedAt": "2026-09-08T00:00:00Z",
            }

        return {
            "commodity": commodity,
            "price": None,
            "location": location or "Local Market",
            "unit": "unknown",
            "source": "demo-market-feed",
            "updatedAt": "2026-09-08T00:00:00Z",
        }
