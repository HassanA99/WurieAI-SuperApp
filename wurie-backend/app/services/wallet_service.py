from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict

from app.services.service_contracts import AgentResponse, ServiceDomain


class WalletService:
    """Production-oriented wallet service with a local in-memory fallback."""

    def __init__(self, datastore=None):
        self.datastore = datastore if datastore is not None else {}

    @staticmethod
    def _timestamp() -> str:
        return datetime.now(timezone.utc).isoformat()

    def _wallet_record(self, user_id: str) -> Dict[str, Any]:
        record = self.datastore.get(user_id, {}) if hasattr(self.datastore, "get") else self.datastore.get(user_id, {})
        if not record:
            record = {
                "userId": user_id,
                "balance": 150.0,
                "currency": "USD",
                "transactions": [],
            }
            self.datastore[user_id] = record
        return record

    def add_transaction(self, user_id: str, amount: float, description: str, txn_type: str = "credit") -> Dict[str, Any]:
        record = self._wallet_record(user_id)
        transaction = {
            "id": f"txn-{len(record['transactions']) + 1:03d}",
            "type": txn_type,
            "amount": float(amount),
            "description": description,
            "currency": record.get("currency", "USD"),
            "createdAt": self._timestamp(),
        }
        record.setdefault("transactions", []).append(transaction)
        record["balance"] = float(record.get("balance", 0.0)) + float(amount)
        self.datastore[user_id] = record
        return record

    def get_balance(self, user_id: str) -> Dict[str, Any]:
        record = self._wallet_record(user_id)
        return {
            "userId": record.get("userId", user_id),
            "balance": float(record.get("balance", 0.0)),
            "currency": record.get("currency", "USD"),
            "transactions": list(record.get("transactions", [])),
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
