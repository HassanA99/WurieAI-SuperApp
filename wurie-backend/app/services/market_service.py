from typing import Dict, Any

from app.services.service_contracts import AgentResponse, ServiceDomain


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

    def lookup(self, query: str) -> AgentResponse:
        """Return a canonical AgentResponse for market or product price requests."""
        low = query.lower()
        commodity = "rice"
        if "fish" in low:
            commodity = "fish"
        elif "cement" in low:
            commodity = "cement"

        price = self.get_price(commodity, "Freetown")
        material = price.get("commodity") or commodity
        text = f"The current price of {material} is {price['price']} per {price['unit']} in {price['location']}."
        return AgentResponse(
            text=text,
            action="NAVIGATE_TO",
            target="Market Prices",
            data={
                "commodity": material,
                "price": price.get("price"),
                "location": price.get("location"),
                "unit": price.get("unit"),
                "source": price.get("source"),
            },
            service=ServiceDomain.MARKET,
        )
