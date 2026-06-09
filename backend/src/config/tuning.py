"""Runtime tuning constants kept out of business logic."""

from __future__ import annotations

FALLBACK_EVENT_LIMIT = 50

LLM_TEMPERATURE = 0.2

PGVECTOR_RECALL_LIMIT = 200
RETRIEVAL_CANDIDATE_MULTIPLIER = 8

FILTER_SCORE_CATEGORY = 3.0
FILTER_SCORE_SKIN_TYPE = 2.0
FILTER_SCORE_BRAND = 1.5
FILTER_SCORE_BRAND_PREFER = 3.0
FILTER_SCORE_BUDGET = 1.0
FILTER_SCORE_SCENARIO = 0.5

CHEAPER_BUDGET_DEFAULT_MAX = 100.0
CHEAPER_BUDGET_RATIO = 0.5
CHEAPER_BUDGET_MIN_MAX = 50.0

# Progressive budget relaxation: (ratio, label). None = remove budget entirely.
# Each step is tried in order; product_type and category are NEVER relaxed.
BUDGET_RELAXATION_STEPS: tuple[tuple[float | None, str], ...] = (
    (1.3, "budget_relaxed_30pct"),
    (1.5, "budget_relaxed_50pct"),
    (None, "budget_relaxed_unlimited"),
)
# Penalty factor for products above the original budget: excess_pct * factor.
BUDGET_PENALTY_FACTOR = 10.0
# Maximum penalty to prevent a single over-budget product from being buried entirely.
BUDGET_PENALTY_CAP = 5.0

# Product card emission pacing (ms)
INTER_CARD_DELAY_MS = 150

# Decision confidence threshold: score gap below this ratio of max is "low confidence"
DECISION_LOW_CONFIDENCE_RATIO = 0.15
DECISION_MIN_USER_SIGNALS_FOR_HIGH_CONFIDENCE = 2

# Default timeout for LLM/embedding/rerank service calls (seconds)
DEFAULT_SERVICE_TIMEOUT_SECONDS = 30.0

# SSE delta passthrough poll interval (seconds)
SSE_DELTA_POLL_TIMEOUT_SECONDS = 0.05

# Default criteria ID when none is assigned
DEFAULT_CRITERIA_ID = "c_auto_001"

LEGACY_MAIN_CHUNK_MAX_CHARS = 400
LEGACY_SUB_CHUNK_MAX_CHARS = 200
LEGACY_HEADER_MAX_CHARS = 50
SEMANTIC_CHUNK_MAX_CHARS = 480
MARKETING_CHUNK_MAX_CHARS = 420
SUMMARY_SENTENCE_MAX_CHARS = 160
ALIAS_MAX_COUNT = 10
DEDUP_VALUE_MAX_CHARS = 80
