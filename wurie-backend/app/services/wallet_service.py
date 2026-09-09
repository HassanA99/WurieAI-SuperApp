from typing import Dict, Any

from app.services.service_contracts import AgentResponse, ServiceDomain


class WalletService:
    """Production-oriented wallet service skeleton."""

    def __init__(self, datastore=None):
        self.datastore = datastore

    def get_balance(self, user_id: str) -> Dict[str, Any]:
        return {
            "userId": user_id,
            "balance": 150.0,
            "currency": "USD",
            "transactions": [],
        }

    def balance(self, query: str) -> AgentResponse:
        """Return a canonical structured wallet balance response."""
        return AgentResponse(
            text="Your current wallet balance is $150.00.",
            action="SHOW_WALLET",
            target="Wallet",
            data={
                "balance": 150.0,
                "currency": "USD",
                "user_id": "unknown",
                "transactions": [],
            },
            service=ServiceDomain.WALLET,
        )
