from __future__ import annotations

from typing import Any, Dict, List, Optional


class FirestoreRepository:
    """Small Firestore adapter with an in-memory fallback for local development."""

    def __init__(self, collection: str, client: Any | None = None) -> None:
        self.collection = collection
        self._documents: Dict[str, Dict[str, Any]] = {}
        self._collection = client.collection(collection) if client is not None else None

    @property
    def uses_firestore(self) -> bool:
        return self._collection is not None

    def __len__(self) -> int:
        if self._collection is not None:
            return len(self.get_all())
        return len(self._documents)

    def __getitem__(self, key: str) -> Dict[str, Any]:
        document = self.get_by_id(key)
        if document is None:
            raise KeyError(key)
        return document

    def __setitem__(self, key: str, value: Dict[str, Any]) -> None:
        if self._collection is not None:
            self._collection.document(key).set(value)
            return
        self._documents[key] = value

    def __contains__(self, key: str) -> bool:
        return key in self._documents

    def get(self, doc_id: str, default: Optional[Dict[str, Any]] = None) -> Optional[Dict[str, Any]]:
        if self._collection is not None:
            return self.get_by_id(doc_id) or default
        return self._documents.get(doc_id, default)

    def values(self) -> List[Dict[str, Any]]:
        if self._collection is not None:
            return self.get_all()
        return list(self._documents.values())

    def get_all(self) -> List[Dict[str, Any]]:
        if self._collection is not None:
            return [doc.to_dict() | {"id": doc.id} for doc in self._collection.stream()]
        return list(self._documents.values())

    def get_by_id(self, doc_id: str) -> Optional[Dict[str, Any]]:
        if self._collection is not None:
            snapshot = self._collection.document(doc_id).get()
            return snapshot.to_dict() | {"id": snapshot.id} if snapshot.exists else None
        return self._documents.get(doc_id)

    def insert(self, doc: Dict[str, Any]) -> Dict[str, Any]:
        doc_id = doc.get("providerId") or doc.get("id") or str(len(self._documents) + 1)
        if self._collection is not None:
            self._collection.document(doc_id).set(doc)
            return doc
        self._documents[doc_id] = doc
        return doc

    def update(self, doc_id: str, doc: Dict[str, Any]) -> Dict[str, Any]:
        if self._collection is not None:
            self._collection.document(doc_id).set(doc, merge=True)
            return self.get_by_id(doc_id) or {"id": doc_id, **doc}
        self._documents[doc_id] = {**self._documents.get(doc_id, {}), **doc}
        return self._documents[doc_id]
