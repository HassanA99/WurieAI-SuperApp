from __future__ import annotations

from typing import Any, Dict, List, Optional


class FirestoreRepository:
    """Repository adapter skeleton for Firebase Firestore.

    This file intentionally models the data persistence interface so the
    service adapters can later point to real Firestore collections.
    """

    def __init__(self, collection: str) -> None:
        self.collection = collection

    def get_all(self) -> List[Dict[str, Any]]:
        return []

    def get_by_id(self, doc_id: str) -> Optional[Dict[str, Any]]:
        return None

    def insert(self, doc: Dict[str, Any]) -> Dict[str, Any]:
        return doc

    def update(self, doc_id: str, doc: Dict[str, Any]) -> Dict[str, Any]:
        return {"id": doc_id, **doc}
