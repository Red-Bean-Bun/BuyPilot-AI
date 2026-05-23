"""In-process cancellation registry for active chat turns."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from threading import Lock


class StreamCancelled(Exception):
    """Raised inside the stream generator when the client cancels a turn."""


@dataclass
class CancellationToken:
    session_id: str
    turn_id: str
    _event: asyncio.Event = field(default_factory=asyncio.Event)

    @property
    def cancelled(self) -> bool:
        return self._event.is_set()

    def cancel(self) -> None:
        self._event.set()

    def raise_if_cancelled(self) -> None:
        if self.cancelled:
            raise StreamCancelled(f"Turn {self.turn_id} was cancelled.")


_ACTIVE_TURNS: dict[tuple[str, str], CancellationToken] = {}
_LOCK = Lock()


def register_turn(session_id: str, turn_id: str) -> CancellationToken:
    token = CancellationToken(session_id=session_id, turn_id=turn_id)
    with _LOCK:
        _ACTIVE_TURNS[(session_id, turn_id)] = token
    return token


def unregister_turn(session_id: str, turn_id: str) -> None:
    with _LOCK:
        _ACTIVE_TURNS.pop((session_id, turn_id), None)


def cancel_turn(session_id: str, turn_id: str) -> bool:
    with _LOCK:
        token = _ACTIVE_TURNS.get((session_id, turn_id))
    if token is None:
        return False
    token.cancel()
    return True


def active_turn_count() -> int:
    with _LOCK:
        return len(_ACTIVE_TURNS)
