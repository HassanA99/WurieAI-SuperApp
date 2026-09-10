from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from app.services.service_contracts import CommentItem, NotificationItem


_comments: dict[str, list[dict[str, Any]]] = {}
_notifications: dict[str, list[dict[str, Any]]] = {}


class SocialService:
    """User-scoped social feed state for comments and notifications."""

    def _timestamp(self) -> str:
        return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    def add_comment(self, uid: str, body: str, username: str = "Wurie User") -> CommentItem:
        item = CommentItem(
            id=f"comment-{len(self.list_comments(uid)) + 1}",
            user_id=uid,
            username=username,
            body=body,
            created_at=self._timestamp(),
        )
        _comments.setdefault(uid, []).append(item.model_dump())
        return item

    def list_comments(self, uid: str) -> list[CommentItem]:
        raw_items = _comments.get(uid, [])
        return [CommentItem(**item) for item in raw_items]

    def add_notification(
        self,
        uid: str,
        title: str,
        body: str,
        category: str = "general",
    ) -> NotificationItem:
        item = NotificationItem(
            id=f"notification-{len(self.list_notifications(uid)) + 1}",
            title=title,
            body=body,
            read=False,
            category=category,
            created_at=self._timestamp(),
        )
        _notifications.setdefault(uid, []).append(item.model_dump())
        return item

    def list_notifications(self, uid: str) -> list[NotificationItem]:
        raw_items = _notifications.get(uid, [])
        return [NotificationItem(**item) for item in raw_items]

    def mark_notification_read(self, uid: str, notification_id: str) -> NotificationItem:
        for item in _notifications.get(uid, []):
            if item["id"] == notification_id:
                item["read"] = True
                return NotificationItem(**item)
        raise KeyError(f"Notification {notification_id} not found for uid {uid}")

    def mark_notification_read_by_id(self, uid: str, notification_id: str) -> NotificationItem:
        return self.mark_notification_read(uid, notification_id)
