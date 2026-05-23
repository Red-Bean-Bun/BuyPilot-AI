"""Run blocking service/repo work without blocking the SSE event loop."""

from __future__ import annotations

import asyncio
import queue
import threading
from collections.abc import Callable
from typing import TypeVar

T = TypeVar("T")


async def run_sync_io(fn: Callable[..., T], *args, poll_seconds: float = 0.01, **kwargs) -> T:
    """Run a synchronous callable in a daemon thread and await its result.

    `asyncio.to_thread` should normally be enough, but the local SQLite/SQLModel
    stack can leave the default executor future unresolved in this environment.
    This small runner keeps the event loop responsive and avoids tying runtime
    correctness to that executor path.
    """

    result_queue: queue.Queue[tuple[bool, T | BaseException]] = queue.Queue(maxsize=1)

    def worker() -> None:
        try:
            result_queue.put((True, fn(*args, **kwargs)))
        except BaseException as exc:  # pragma: no cover - re-raised in caller
            result_queue.put((False, exc))

    thread = threading.Thread(target=worker, name=f"sync-io:{getattr(fn, '__name__', 'call')}", daemon=True)
    thread.start()

    while True:
        try:
            ok, value = result_queue.get_nowait()
            break
        except queue.Empty:
            await asyncio.sleep(poll_seconds)

    if ok:
        return value  # type: ignore[return-value]
    if isinstance(value, BaseException):
        raise value
    raise RuntimeError("Synchronous IO worker failed without an exception.")
