"""Small helpers shared by chat stream handlers."""

from __future__ import annotations

import asyncio
import time
from dataclasses import dataclass, field
from typing import Any, AsyncGenerator, Awaitable, Callable, Generic, Mapping, Protocol, TypeVar

from src.runtime.cancel_registry import CancellationToken
from src.runtime.stages.recommendation import RetrievalResult
from src.services.cancellation import is_chat_turn_cancellation_requested
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import (
    CriteriaPayload,
    DoneEvent,
    EventSeq,
    EvidencePayload,
    ProductPayload,
    SSEEventBase,
    ThinkingEvent,
    now_ms,
)

T = TypeVar("T")


class RunRetrieval(Protocol):
    def __call__(
        self,
        criteria: CriteriaPayload,
        top_n: int = 5,
        feedback: Mapping[str, list[str]] | None = None,
    ) -> Awaitable[RetrievalResult]: ...


class StageBundle(Protocol):
    @property
    def run_multimodal(self) -> Callable[[str | None], Awaitable[dict[str, Any] | None]]: ...

    @property
    def run_intent(self) -> Callable[[ChatStreamRequest], Awaitable[IntentResult]]: ...

    @property
    def run_criteria(self) -> Callable[[str, ChatStreamRequest, IntentResult], Awaitable[CriteriaPayload]]: ...

    @property
    def run_retrieval(self) -> RunRetrieval: ...

    @property
    def run_recommendation_text(
        self,
    ) -> Callable[
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None],
        Awaitable[RecommendationResult],
    ]: ...

    @property
    def run_decision(
        self,
    ) -> Callable[
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None], Awaitable[DecisionResult]
    ]: ...


@dataclass(frozen=True)
class StageResult(Generic[T]):
    value: T


@dataclass(frozen=True)
class TimedTask(Generic[T]):
    task: asyncio.Future[T]
    started_at: float


@dataclass
class StreamContext:
    session_id: str
    turn_id: str
    deck_id: str
    seq: EventSeq
    cancel_token: CancellationToken
    stages: StageBundle
    heartbeat_interval_seconds: float
    stage_timings_ms: dict[str, float] = field(default_factory=dict)
    background_tasks: list[TimedTask[Any]] = field(default_factory=list)

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
    awaitable: Awaitable[T],
    timing_key: str | None = None,
    background: bool = False,
) -> TimedTask[T]:
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
    awaitable: Awaitable[T],
    stage: str,
    message: str,
    timing_key: str | None = None,
) -> AsyncGenerator[SSEEventBase | StageResult[T], None]:
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
    timed_task: TimedTask[T],
    stage: str,
    message: str,
    timing_key: str | None = None,
) -> AsyncGenerator[SSEEventBase | StageResult[T], None]:
    task = timed_task.task
    try:
        while True:
            await ensure_active(ctx)
            done, _ = await asyncio.wait({task}, timeout=ctx.heartbeat_interval_seconds)
            await ensure_active(ctx)
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


def cancel_background_tasks(timed_tasks: list[TimedTask[Any]]) -> None:
    for timed_task in timed_tasks:
        if not timed_task.task.done():
            timed_task.task.cancel()


async def ensure_active(ctx: StreamContext) -> None:
    ctx.ensure_active()
    if await is_chat_turn_cancellation_requested(ctx.session_id, ctx.turn_id):
        ctx.cancel_token.cancel()
    ctx.ensure_active()


def _record_stage_timing(stage_timings_ms: dict[str, float], timing_key: str, started_at: float) -> None:
    stage_timings_ms.setdefault(timing_key, round((time.perf_counter() - started_at) * 1000, 2))
