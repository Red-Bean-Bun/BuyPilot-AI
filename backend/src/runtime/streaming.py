"""Small helpers shared by chat stream handlers."""

from __future__ import annotations

import asyncio
import time
from dataclasses import dataclass, field
from typing import Any, AsyncGenerator, Awaitable

from src.runtime.cancel_registry import CancellationToken
from src.types.sse_events import DoneEvent, EventSeq, SSEEventBase, ThinkingEvent, now_ms


@dataclass(frozen=True)
class StageResult:
    value: Any


@dataclass(frozen=True)
class TimedTask:
    task: asyncio.Future[Any]
    started_at: float


@dataclass
class StreamContext:
    session_id: str
    turn_id: str
    deck_id: str
    seq: EventSeq
    cancel_token: CancellationToken
    stages: Any
    heartbeat_interval_seconds: float
    stage_timings_ms: dict[str, float] = field(default_factory=dict)
    background_tasks: list[TimedTask] = field(default_factory=list)

    def ensure_active(self) -> None:
        self.cancel_token.raise_if_cancelled()

    def thinking(self, stage: str, message: str) -> ThinkingEvent:
        self.ensure_active()
        return ThinkingEvent(
            session_id=self.session_id,
            turn_id=self.turn_id,
            seq=self.seq.next(),
            event_id=self.seq.event_id(),
            node_id=f"thinking_{self.turn_id}",
            created_at_ms=now_ms(),
            stage=stage,
            message=message,
        )

    def done(self) -> DoneEvent:
        return DoneEvent(
            session_id=self.session_id,
            turn_id=self.turn_id,
            seq=self.seq.next(),
            event_id=self.seq.event_id(),
            node_id=f"done_{self.turn_id}",
            created_at_ms=now_ms(),
        )


def start_stage_task(
    ctx: StreamContext,
    awaitable: Awaitable[Any],
    timing_key: str | None = None,
    background: bool = False,
) -> TimedTask:
    ctx.ensure_active()
    started_at = time.perf_counter()
    task = asyncio.ensure_future(awaitable)
    if timing_key:
        task.add_done_callback(lambda _: _record_stage_timing(ctx.stage_timings_ms, timing_key, started_at))
    timed_task = TimedTask(task=task, started_at=started_at)
    if background:
        ctx.background_tasks.append(timed_task)
    return timed_task


async def run_with_heartbeat(
    ctx: StreamContext,
    awaitable: Awaitable[Any],
    stage: str,
    message: str,
    timing_key: str | None = None,
) -> AsyncGenerator[SSEEventBase | StageResult, None]:
    timed_task = start_stage_task(ctx, awaitable, timing_key=timing_key)
    async for item in run_timed_task_with_heartbeat(
        ctx,
        timed_task,
        stage,
        message,
        timing_key=timing_key,
    ):
        yield item


async def run_timed_task_with_heartbeat(
    ctx: StreamContext,
    timed_task: TimedTask,
    stage: str,
    message: str,
    timing_key: str | None = None,
) -> AsyncGenerator[SSEEventBase | StageResult, None]:
    task = timed_task.task
    try:
        while True:
            ctx.ensure_active()
            done, _ = await asyncio.wait({task}, timeout=ctx.heartbeat_interval_seconds)
            ctx.ensure_active()
            if task in done:
                if timing_key:
                    _record_stage_timing(ctx.stage_timings_ms, timing_key, timed_task.started_at)
                yield StageResult(task.result())
                return
            yield ctx.thinking(stage, message)
    except BaseException:
        if not task.done():
            task.cancel()
        raise


def cancel_background_tasks(timed_tasks: list[TimedTask]) -> None:
    for timed_task in timed_tasks:
        if not timed_task.task.done():
            timed_task.task.cancel()


def _record_stage_timing(stage_timings_ms: dict[str, float], timing_key: str, started_at: float) -> None:
    stage_timings_ms.setdefault(timing_key, round((time.perf_counter() - started_at) * 1000, 2))
