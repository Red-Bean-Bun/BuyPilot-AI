"""ShoppingStrategyService — P0 deterministic scenario strategy.

Responsibilities:
- Identify gift / interest scenarios from user message
- Compute decision_barrier (fear_wrong_choice / price_sensitive / ...)
- Generate a viable primary_direction (category + product_type + use_scenario)
- Lightweight feasibility check via retrieval_probe
- Preserve user's explicit constraints (never override)

P0 implementation is fully deterministic. `prompts/shopping_strategy.md` is
a placeholder for P1 LLM-based direction generation.
"""

from __future__ import annotations

import uuid
from collections.abc import Awaitable, Callable
from typing import Literal, Protocol

from pydantic import BaseModel

from src.config.domain_terms import contains_any, has_negation_prefix
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import (
    CriteriaPayload,
    DecisionBarrierPayload,
    PrimaryDirectionPayload,
    SearchStrategyPayload,
    ShoppingStrategyPayload,
)


# ---------------------------------------------------------------------------
# Types
# ---------------------------------------------------------------------------


class RetrievalProbeResult(Protocol):
    """Structural subset of runtime's RetrievalResult for feasibility probe."""

    @property
    def products(self) -> list: ...


RetrievalProbeFn = Callable[[CriteriaPayload], Awaitable[RetrievalProbeResult]]


class ShoppingStrategyPlan(BaseModel):
    """Output of `build_shopping_strategy_plan`.

    - `route`: scenario_strategy (full) or scenario_filter (preserve explicit constraints)
    - `scene_judgement_text`: 1~2 sentence scene judgement (emitted as text_delta)
    - `criteria`: input criteria + strategy-filled unspecified fields
    - `shopping_strategy`: structured payload for criteria_card.shopping_strategy
    - `reason_hint`: optional hint for downstream recommendation text
    """

    route: Literal["scenario_strategy", "scenario_filter"]
    scene_judgement_text: str
    criteria: CriteriaPayload
    shopping_strategy: ShoppingStrategyPayload
    reason_hint: str | None = None


# ---------------------------------------------------------------------------
# Keyword sets
# ---------------------------------------------------------------------------


_GIFT_RECIPIENTS = ("男朋友", "女朋友", "老公", "老婆", "妈妈", "爸爸", "父亲", "母亲", "儿子", "女儿", "朋友", "同事", "长辈", "孩子")
_GIFT_OCCASIONS = ("生日", "礼物", "送礼", "送礼物", "纪念日", "情人节", "母亲节", "父亲节", "圣诞节", "新年", "春节", "七夕")
_GIFT_VERBS = ("送给", "送个", "送一款", "送一件", "送一份")

_INTEREST_GENERIC = ("电子产品", "数码", "美妆", "护肤", "运动", "健身", "跑步", "拍照", "摄影", "游戏", "穿搭")
# Specific product terms belong to _FILTER_PRODUCT_TYPES (filter signal, not scene interest).
_INTEREST_SPECIFIC = (
    "足球",
    "篮球",
    "网球",
    "羽毛球",
    "游泳",
    "骑行",
    "登山",
    "滑雪",
    "瑜伽",
)
_INTEREST_ALL = _INTEREST_GENERIC + _INTEREST_SPECIFIC

_UNCERTAINTY = (
    "怎么选",
    "选哪个",
    "不知道",
    "不确定",
    "推荐",
    "有什么好",
    "送什么",
    "买什么",
    "怕踩雷",
    "踩雷",
    "怕送错",
    "挑花眼",
    "不知道送",
    "不知道买",
)

_FILTER_PRODUCT_TYPES = ("耳机", "蓝牙耳机", "手机", "平板", "电脑", "相机", "手表", "音箱", "音响", "防晒霜", "洗面奶", "面膜", "口红", "精华", "跑鞋", "运动鞋", "T恤")
_BUDGET_TERMS = ("元内", "以内", "预算", "块以内", "块钱", "元左右")
_BRAND_TERMS = ("小米", "华为", "OPPO", "vivo", "三星", "苹果", "iPhone", "索尼", "尼康", "佳能", "兰蔻", "雅诗兰黛", "SK-II", "资生堂", "Nike", "Adidas", "耐克", "阿迪达斯")


# ---------------------------------------------------------------------------
# Scene / filter scoring
# ---------------------------------------------------------------------------


def _count_hits(text: str, terms: tuple[str, ...]) -> int:
    """Count how many terms appear in text without negation prefix."""
    return sum(1 for term in terms if term in text and not has_negation_prefix(text, term))


def _compute_scene_score(text: str) -> int:
    score = 0
    if _count_hits(text, _GIFT_RECIPIENTS) >= 1:
        score += 2
    if _count_hits(text, _GIFT_OCCASIONS) >= 1 or _count_hits(text, _GIFT_VERBS) >= 1:
        score += 2
    if _count_hits(text, _UNCERTAINTY) >= 1:
        score += 2
    if _count_hits(text, _INTEREST_ALL) >= 1:
        score += 1
    return min(score, 8)


def _compute_filter_score(text: str, extracted: dict) -> int:
    score = 0
    if extracted.get("product_type"):
        score += 3
    elif _count_hits(text, _FILTER_PRODUCT_TYPES) >= 1:
        score += 3
    if extracted.get("budget_max") is not None or extracted.get("budget_min") is not None:
        score += 2
    elif contains_any(text, _BUDGET_TERMS):
        score += 2
    if extracted.get("brand_prefer") or extracted.get("brand_avoid"):
        score += 2
    elif _count_hits(text, _BRAND_TERMS) >= 1:
        score += 2
    hard_fields = {"storage", "screen_size", "skin_type", "sport_type", "season", "dietary", "ingredient_avoid", "origin_avoid"}
    if any(extracted.get(f) for f in hard_fields):
        score += 2
    return score


# ---------------------------------------------------------------------------
# Scene classification
# ---------------------------------------------------------------------------


def _classify_scene_type(text: str) -> str | None:
    if _count_hits(text, _GIFT_RECIPIENTS + _GIFT_OCCASIONS + _GIFT_VERBS) >= 1:
        return "gift"
    if _count_hits(text, _INTEREST_ALL) >= 1:
        return "interest"
    return None


# ---------------------------------------------------------------------------
# Decision barrier
# ---------------------------------------------------------------------------


_PRICE_SIGNALS = ("预算", "便宜", "更便宜", "太贵", "性价比", "省钱", "实惠")
_FIT_SIGNALS = ("敏感肌", "肤质", "尺码", "尺寸", "大小", "合适", "适配", "过敏")
_TRUST_SIGNALS = ("靠谱", "真假", "正品", "质量", "评测", "口碑")
_CHOICE_SIGNALS = ("怎么选", "挑花眼", "太多", "选择困难")


def _detect_barrier(text: str, extracted: dict, scene_type: str) -> DecisionBarrierPayload | None:
    if contains_any(text, _PRICE_SIGNALS) or extracted.get("budget_max") is not None:
        return DecisionBarrierPayload(
            barrier_type="price_sensitive",
            label="担心预算不够或价格偏高",
            reason="用户有明确预算约束，需要在预算内找到最优选择",
            conversion_strategy="在预算范围内筛选，并说明性价比逻辑",
        )
    if contains_any(text, _FIT_SIGNALS) or extracted.get("skin_type"):
        return DecisionBarrierPayload(
            barrier_type="fit_uncertainty",
            label="担心不适合肤质或场景",
            reason="涉及肤质/尺寸等个体差异，选错会有实际使用风险",
            conversion_strategy="优先推荐低风险温和方向，并提示查看风险说明",
        )
    if contains_any(text, _TRUST_SIGNALS):
        return DecisionBarrierPayload(
            barrier_type="trust_uncertainty",
            label="不确定推荐是否靠谱",
            reason="用户希望看到依据，担心被营销话术误导",
            conversion_strategy="优先给查看依据入口，用证据链建立信任",
        )
    # Gift-specific fallback: in gift context, "怎么选/不知道" reads as fear of wrong choice,
    # not generic choice overload. Must come before choice_overload to avoid misclassification.
    if scene_type == "gift":
        return DecisionBarrierPayload(
            barrier_type="fear_wrong_choice",
            label="怕送错、怕不够体面",
            reason="礼物场景下核心设备容易踩型号偏好，送礼风险高",
            conversion_strategy="先推荐低偏好依赖、礼物感更强的小件，并保留查看依据和换方向入口",
        )
    if contains_any(text, _CHOICE_SIGNALS):
        return DecisionBarrierPayload(
            barrier_type="choice_overload",
            label="候选太多，不知道怎么选",
            reason="候选过多反而增加决策成本，需要帮用户收敛",
            conversion_strategy="收敛到 1 个主方向 + 少量对比候选",
        )
    return None


# ---------------------------------------------------------------------------
# Direction generation
# ---------------------------------------------------------------------------


def _has_interest(text: str, terms: tuple[str, ...]) -> bool:
    return any(t in text and not has_negation_prefix(text, t) for t in terms)


def _pick_interest_direction(text: str) -> tuple[str, str | None, str | None]:
    """Return (title, product_type_or_None, use_scenario)."""
    if _has_interest(text, ("足球", "篮球", "网球", "羽毛球", "运动", "健身")):
        return "低风险运动休闲单品", "短袖T恤", "运动休闲"
    if _has_interest(text, ("拍照", "摄影", "相机")):
        return "低门槛影像配件", None, "日常记录"
    if _has_interest(text, ("游戏",)):
        return "轻量游戏配件", None, "游戏娱乐"
    if _has_interest(text, ("电子产品", "数码")):
        return "低踩雷的黑科技小件", None, "日常使用"
    if _has_interest(text, ("美妆", "护肤")):
        return "温和低风险的护肤入门", None, "日常护肤"
    if _has_interest(text, ("穿搭",)):
        return "日常百搭单品", None, "日常穿搭"
    return "场景相关单品", None, None


def _build_direction(
    scene_type: str,
    text: str,
    category: str | None,
    criteria: CriteriaPayload,
) -> PrimaryDirectionPayload:
    if scene_type == "gift":
        if _has_interest(text, ("电子产品", "数码", "耳机", "手机", "平板", "电脑", "相机", "手表")):
            return PrimaryDirectionPayload(
                title="低踩雷的黑科技小件",
                summary="优先考虑音频配件或轻量数码配件",
                why="有新鲜感，不强依赖具体型号偏好，也更适合生日礼物",
                search_strategy=SearchStrategyPayload(
                    category="数码电子",
                    product_type="真无线耳机",
                    use_scenario="日常使用",
                ),
            )
        if _has_interest(text, ("足球", "篮球", "运动", "健身", "跑步")):
            return PrimaryDirectionPayload(
                title="低风险运动休闲礼物",
                summary="当前商品库里的运动休闲上衣",
                why="不强绑定具体球队或运动偏好，日常能穿，作为稳妥礼物比随机运动装备更低风险",
                search_strategy=SearchStrategyPayload(
                    category="服饰运动",
                    product_type="短袖T恤",
                    use_scenario="运动休闲",
                ),
            )
        if _has_interest(text, ("美妆", "护肤", "洗面奶", "防晒霜", "面膜")) or (category == "美妆护肤"):
            return PrimaryDirectionPayload(
                title="温和低风险的护肤入门",
                summary="优先温和保湿、低刺激方向",
                why="送护肤品最怕肤质和刺激风险，温和方向适用面广，踩雷概率低",
                search_strategy=SearchStrategyPayload(category="美妆护肤", use_scenario="日常护肤"),
            )
        return PrimaryDirectionPayload(
            title="稳妥礼物方向",
            summary="优先考虑低偏好依赖、礼物感强的单品",
            why="在不清楚收礼人具体偏好的情况下，避开强型号/强口味依赖的大件更安全",
            search_strategy=SearchStrategyPayload(category=category or ""),
        )

    # interest scene
    title, product_type, use_scenario = _pick_interest_direction(text)
    return PrimaryDirectionPayload(
        title=title,
        summary=f"基于对方{criteria.constraints.product_type or '兴趣'}偏好推荐相关方向",
        why="围绕兴趣偏好推荐，比随机品类更贴近用户需求",
        search_strategy=SearchStrategyPayload(
            category=category or "",
            product_type=product_type,
            use_scenario=use_scenario,
        ),
    )


# ---------------------------------------------------------------------------
# Scene judgement text
# ---------------------------------------------------------------------------


def _scene_judgement_text(scene_type: str, barrier: DecisionBarrierPayload | None, direction_title: str) -> str:
    barrier_key = barrier.barrier_type if barrier else ""
    if scene_type == "gift":
        if barrier_key == "fear_wrong_choice":
            return f"这不是单纯买商品，而是送礼物。核心设备容易踩型号偏好，我会先避开强型号依赖的大件，优先看{direction_title}方向。"
        if barrier_key == "price_sensitive":
            return f"礼物要体面也要控预算。我会在预算内优先看{direction_title}方向，让性价比和礼物感都站得住。"
        if barrier_key == "fit_uncertainty":
            return f"送护肤品最怕踩肤质雷。我会优先看{direction_title}方向，降低不适用风险。"
        return f"我会优先看{direction_title}方向，让礼物更稳。"

    if scene_type == "interest":
        if barrier_key == "choice_overload":
            return f"围绕这个兴趣，我先收敛到{direction_title}方向，减少选择成本。"
        return f"围绕这个兴趣，我会优先看{direction_title}方向。"

    return f"我会优先看{direction_title}方向。"


# ---------------------------------------------------------------------------
# Criteria merging
# ---------------------------------------------------------------------------


def _merge_strategy_criteria(criteria: CriteriaPayload, direction: PrimaryDirectionPayload) -> CriteriaPayload:
    """Strategy fills unspecified fields. Explicit user constraints are preserved."""
    search = direction.search_strategy
    updates: dict = {}
    if not criteria.category and search.category:
        updates["category"] = search.category
    constraint_updates: dict = {}
    if not criteria.constraints.product_type and search.product_type:
        constraint_updates["product_type"] = search.product_type
    if not criteria.constraints.use_scenario and search.use_scenario:
        constraint_updates["use_scenario"] = search.use_scenario
    if constraint_updates:
        updates["constraints"] = criteria.constraints.model_copy(update=constraint_updates)
    return criteria.model_copy(update=updates) if updates else criteria


# ---------------------------------------------------------------------------
# Small helpers
# ---------------------------------------------------------------------------


def _category_hint_present(intent: IntentResult) -> bool:
    return bool(intent.category)


def _avoid_risks_for_barrier(barrier: DecisionBarrierPayload | None) -> list[str]:
    if barrier is None:
        return []
    if barrier.barrier_type == "fear_wrong_choice":
        return [
            "不要盲买手机、电脑这类强型号偏好的大件",
            "不知道常用品牌时，优先选兼容性更强的小件",
        ]
    if barrier.barrier_type == "price_sensitive":
        return ["不要在预算外硬推高配，避免用户后续反悔"]
    if barrier.barrier_type == "fit_uncertainty":
        return ["不确定肤质/尺码时，优先选低风险温和方向"]
    return []


def _assumptions_for_scene(extracted: dict) -> list[str]:
    assumptions: list[str] = []
    if extracted.get("budget_max") is None and extracted.get("budget_min") is None:
        assumptions.append("暂时不知道预算")
    if not extracted.get("brand_prefer"):
        assumptions.append("暂时不知道对方常用品牌")
    if not extracted.get("skin_type"):
        assumptions.append("暂时不知道对方肤质")
    return assumptions


def _scene_summary(scene_type: str, text: str) -> str:
    if scene_type == "gift":
        if "男朋友" in text:
            return "送男朋友礼物"
        if "女朋友" in text:
            return "送女朋友礼物"
        if "妈妈" in text or "母亲" in text:
            return "送妈妈礼物"
        if "爸爸" in text or "父亲" in text:
            return "送爸爸礼物"
        if "朋友" in text:
            return "送朋友礼物"
        return "送礼物场景"
    if scene_type == "interest":
        for term in _INTEREST_ALL:
            if term in text:
                return f"围绕{term}兴趣的选购"
        return "兴趣导向选购"
    return "场景化选购"


def _user_problem(scene_type: str, barrier: DecisionBarrierPayload | None) -> str:
    if scene_type == "gift":
        if barrier and barrier.barrier_type == "fear_wrong_choice":
            return "用户不确定这个场景下送什么更体面、更有心意、更不容易踩雷"
        if barrier and barrier.barrier_type == "price_sensitive":
            return "用户希望在预算内送出体面又不踩雷的礼物"
        return "用户希望在礼物场景下做出稳妥选择"
    if scene_type == "interest":
        return "用户希望在兴趣相关品类里找到更贴合偏好的选择"
    return "用户希望做出更稳妥的购买决策"


def build_scenario_reason_hint(strategy: ShoppingStrategyPayload) -> str:
    """Short product-card reason for scenario flow.

    Product card reasons are compact UI copy, so keep them well under the PRD
    limit and avoid restating generic field matching.
    """
    barrier = strategy.decision_barrier.barrier_type if strategy.decision_barrier else ""
    if barrier == "fear_wrong_choice":
        return "低偏好依赖，送礼更稳"
    if barrier == "price_sensitive":
        return "预算更可控"
    if barrier == "fit_uncertainty":
        return "风险点清楚，适合核对"
    if barrier == "choice_overload":
        return "礼物感和实用性更平衡"
    if barrier == "trust_uncertainty":
        return "证据更清楚，决策更稳"
    title = strategy.primary_direction.title.strip()
    return title[:12] if title else "场景匹配更稳"


# ---------------------------------------------------------------------------
# Public entry
# ---------------------------------------------------------------------------


async def build_shopping_strategy_plan(
    body: ChatStreamRequest,
    intent: IntentResult,
    criteria: CriteriaPayload,
    *,
    retrieval_probe: RetrievalProbeFn | None = None,
) -> ShoppingStrategyPlan | None:
    """Build a shopping strategy plan if the user's input triggers scenario routing.

    Returns None when:
    - intent is not `recommend` (non-recommend intents don't enter strategy)
    - scene_type is not gift/interest (P0 hard gate)
    - scene_score and filter_score don't meet scenario routing thresholds
    - no viable direction is found in the catalog after feasibility probe
    """
    text = body.message
    extracted = intent.extracted_constraints or {}

    # P0: non-recommend intent → no strategy
    if intent.intent != "recommend":
        return None

    scene_score = _compute_scene_score(text)
    filter_score = _compute_filter_score(text, extracted)
    scene_type = _classify_scene_type(text)

    # P0: only gift / interest enter scenario routing
    if scene_type not in {"gift", "interest"}:
        return None

    # Route determination per PRD §4.2
    if filter_score >= 5 and scene_score <= 2:
        # Strong filter, weak scene → plain filter_recommend
        return None
    if scene_score <= 2 and filter_score <= 2:
        # Too weak → clarification / broad_recommend
        return None

    # Scenario routes
    if scene_type == "gift" and scene_score >= 4:
        route: Literal["scenario_strategy", "scenario_filter"] = (
            "scenario_strategy" if filter_score <= 3 else "scenario_filter"
        )
    elif scene_type == "interest":
        if scene_score >= 6:
            route = "scenario_strategy" if filter_score <= 3 else "scenario_filter"
        elif scene_score >= 4 and _category_hint_present(intent):
            route = "scenario_strategy" if filter_score <= 3 else "scenario_filter"
        else:
            return None
    else:
        return None

    barrier = _detect_barrier(text, extracted, scene_type)
    direction = _build_direction(scene_type, text, intent.category, criteria)

    # Feasibility probe
    if retrieval_probe is not None:
        probe_criteria = _merge_strategy_criteria(criteria, direction)
        result = await retrieval_probe(probe_criteria)
        count = len(result.products)

        if count == 0:
            # Fallback: broaden to category only
            broad_category = probe_criteria.category or intent.category
            if broad_category and broad_category != probe_criteria.category:
                fallback_criteria = CriteriaPayload(category=broad_category)
                fallback_result = await retrieval_probe(fallback_criteria)
                count = len(fallback_result.products)

            if count == 0:
                return None
            # Use broad fallback direction
            direction = PrimaryDirectionPayload(
                title=f"{broad_category}稳妥方向",
                summary=f"当前商品库在{broad_category}内可支撑",
                why="候选方向在商品库有支撑，比空方向更稳",
                search_strategy=SearchStrategyPayload(category=broad_category),
                available_in_catalog=True,
                supporting_product_count=count,
            )
        else:
            direction = direction.model_copy(
                update={"available_in_catalog": True, "supporting_product_count": count}
            )
    else:
        direction = direction.model_copy(update={"available_in_catalog": True, "supporting_product_count": 0})

    confidence: Literal["low", "medium", "high"] = (
        "high" if scene_score >= 6 else "medium" if scene_score >= 4 else "low"
    )
    judgement = _scene_judgement_text(scene_type, barrier, direction.title)
    final_criteria = _merge_strategy_criteria(criteria, direction)

    strategy = ShoppingStrategyPayload(
        strategy_id=f"scene_{uuid.uuid4().hex[:8]}",
        scene_type=scene_type,  # type: ignore[arg-type]
        scene_summary=_scene_summary(scene_type, text),
        user_problem=_user_problem(scene_type, barrier),
        decision_barrier=barrier,
        primary_direction=direction,
        avoid_risks=_avoid_risks_for_barrier(barrier),
        assumptions=_assumptions_for_scene(extracted),
        confidence=confidence,
    )

    return ShoppingStrategyPlan(
        route=route,
        scene_judgement_text=judgement,
        criteria=final_criteria,
        shopping_strategy=strategy,
        reason_hint=build_scenario_reason_hint(strategy),
    )
