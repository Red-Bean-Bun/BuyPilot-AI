"""Centralised user-facing messages for SSE events and UI labels.

All Chinese text that flows to the frontend via SSE events is defined here.
Business logic (domain_terms.py, cart_rules.py, message_rules.py) keeps its
own matching keywords and regex patterns — those are not display text.
"""

from __future__ import annotations


# ── Thinking stage labels (shown as SSE ThinkingEvent.message) ───────────

THINKING_ANALYZING_IMAGE = "正在分析图片..."
THINKING_UNDERSTANDING = "正在理解您的需求..."
THINKING_GENERATING_CRITERIA = "正在生成购买标准..."
THINKING_SEARCHING = "正在检索匹配商品..."
THINKING_RANKING = "正在排序和筛选候选商品..."
THINKING_DECISION = "正在生成适配建议..."
THINKING_DECISION_WITH_FEEDBACK = "正在结合你的反馈生成最终建议..."
THINKING_VIEWING_CART = "正在查看购物车..."
THINKING_PROCESSING = "正在处理..."
THINKING_NEEDS_INFO = "需要补充一个关键信息。"
THINKING_CONFIRM_CART = "需要确认购物车商品。"

# ── Greeting and general intro ──────────────────────────────────────────

GREETING = "我是电商导购助手，可以帮你推荐商品、添加购物车。请问你想买什么？"
CHITCHAT_HINT = "我是购物助手，可以帮你找商品、比价格、加购物车。你想看哪个品类？"
INTRO_NO_CONSTRAINTS = "正在分析你的需求，马上为你查找匹配商品..."

# ── Recommendation followup text ────────────────────────────────────────

FOLLOWUP_DEFAULT = "你先看看这几款候选。如果想调整筛选范围，可以直接说「预算再低一点」「换个品牌」「想要性价比高的」，也可以点击筛选卡修改。"
FOLLOWUP_SUBSEQUENT = "已根据你的反馈调整了筛选条件，看看这批候选。"
FOLLOWUP_NO_MATCH = "当前条件下暂时没有匹配的商品。你可以放宽预算、换一个品类，或者去掉一些排除条件试试。"
FOLLOWUP_NO_MATCH_NEED_PRODUCT_TYPE_TEMPLATE = (
    "我已经定位到{category}，但还需要知道你具体想买{examples}中的哪一类。你可以直接补充具体商品类型或使用场景。"
)
FOLLOWUP_NO_MATCH_ADJUST_TEMPLATE = "当前筛选条件下暂时没有匹配商品。你可以{suggestions}试试。"
FOLLOWUP_BUDGET_RELAXED = "原预算内没有匹配商品，以下为放宽预算后的备选。如果你想回到原预算，可以直接说或点筛选卡修改。"
RECOMMENDATION_STREAM_FALLBACK = "我先把匹配商品列出来，方便你快速比较。"

# ── Clarification analysis text ─────────────────────────────────────────

CLARIFICATION_ANALYSIS = (
    "我先看了一下你的需求，已经能判断大方向，但还缺一个会影响推荐标准的关键信息。补齐后再生成购买标准会更稳。"
)

# ── No-match / fallback ─────────────────────────────────────────────────

NO_MATCH = "暂时没有找到合适商品。"
NO_SUITABLE_WINNER = "当前候选都不太符合你的偏好。你可以换一组商品，或者调整筛选条件后重新推荐。"

# ── Intro text templates (used with f-string formatting in handlers) ────

INTRO_BUDGET_SUFFIX = "元以内"
INTRO_SKIN_SUFFIX = "肌肤"
INTRO_STORAGE_SUFFIX = "存储"
INTRO_SCREEN_SUFFIX = "屏幕"
INTRO_SPORT_SUFFIX = "运动"
INTRO_CONDITIONS_PREFIX = "我先按"
INTRO_CONDITIONS_INFIX = "这几个条件找一组候选。"
INTRO_SINGLE_CATEGORY_SUFFIX = "看到商品后也可以继续调整筛选范围。"

# ── Clarification questions and options ─────────────────────────────────
# See also: prompts/clarification.md ## DeterministicQuestions section
# These constants are used by slot_checker.py for deterministic clarification.

CLARIFY_CATEGORY_QUESTION = "你想买哪一类商品？"
CLARIFY_BUDGET_QUESTION = "这类商品价格跨度比较大，你的预算或价位范围大概是多少？"
CLARIFY_PRODUCT_TYPE_QUESTION = "你想买具体哪一类商品？"
CLARIFY_FALLBACK_QUESTION = "请补充一下你的购买需求。"

CLARIFY_PRODUCT_TYPE_OPTIONS: tuple[str, ...] = (
    "洁面",
    "防晒",
    "面霜",
    "精华",
    "手机",
    "耳机",
    "跑鞋",
    "T恤",
    "咖啡",
    "茶饮",
    "零食",
)
CLARIFY_BUDGET_OPTIONS: tuple[str, ...] = (
    "1000-2000元",
    "2000-4000元",
    "4000元以上",
)

# ── Quick action labels (product card) ──────────────────────────────────

QA_SHOW_EVIDENCE = "看证据"
QA_ADD_TO_CART = "加入购物车"
QA_DISLIKE = "不喜欢这个"
QA_CHEAPER = "再便宜一点"
QA_COMPARE = "加入对比"

# ── Criteria card quick actions ─────────────────────────────────────────

QA_BUDGET_LOW = "预算压低"
QA_REPLACE_DECK = "换一组"
# Category-specific
QA_SENSITIVE_SKIN = "敏感肌适用"
QA_NO_ALCOHOL = "不要含酒精"
QA_STORAGE_256 = "256G以上"
QA_LARGE_SCREEN = "大屏幕"
QA_RUNNING = "跑步"
QA_SPRING_SUMMER = "春夏款"
QA_SUGAR_FREE = "无糖"
QA_LOW_FAT = "低脂"

# ── Risk notes ──────────────────────────────────────────────────────────

RISK_OVER_BUDGET = "原预算内无匹配，此为超预算备选"

# ── Cart clarification questions ────────────────────────────────────────
# See also: prompts/clarification.md ## DeterministicQuestions ### Cart Clarification

CART_CLARIFY_ADD = "你想把哪个商品加入购物车？"
CART_CLARIFY_REMOVE = "你想从购物车移出哪个商品？"
CART_CLARIFY_UPDATE = "你想修改哪个商品的数量？"

# ── Error messages ──────────────────────────────────────────────────────

PIPELINE_ERROR = "本轮导购处理失败，请稍后重试。"
UNSUPPORTED_PRODUCT_TYPE_TEMPLATE = "当前商品库暂不覆盖{product_type}，我不能基于现有数据给出可靠推荐。"
UNSUPPORTED_PRODUCT_TYPE_FALLBACK = "这类商品"
COMMERCIAL_CLAIM_REPLY = "当前商品库无该字段，不能确认。"

# ── Decision fallback text ──────────────────────────────────────────────

DECISION_SUMMARY_FALLBACK_TEMPLATE = "优先选{winner_id}。"
DECISION_WHY_DEFAULT = "综合匹配度最高"
DECISION_LOCKED_REASON_TEMPLATE = "{name} 是当前候选中综合匹配度最高的商品{score_text}。"
DECISION_LOCKED_REASON_SOURCE = "这个结论来自检索相关性、标准匹配、用户反馈和证据质量的综合评分。"
DECISION_LOCKED_SUMMARY_TEMPLATE = "优先推荐{name}，它是当前候选里综合评分最高的一款。"
DECISION_SAFE_REPLACEMENT = "在当前候选范围内"

# ── Display ordering (ordered list for UI quick-reply buttons) ──────────

CATEGORY_DISPLAY_ORDER: tuple[str, ...] = ("美妆护肤", "数码电子", "服饰运动", "食品生活")
DEFAULT_CATEGORY = "美妆护肤"
PRODUCT_TYPE_HINTS_BY_CATEGORY: dict[str, str] = {
    "美妆护肤": "洁面、防晒、面霜、精华",
    "数码电子": "手机、耳机、平板、笔记本电脑",
    "服饰运动": "跑步鞋、篮球鞋、T恤、背包",
    "食品生活": "茶饮、咖啡、零食、调味品",
}

# ── Compare stage labels ─────────────────────────────────────────────

THINKING_COMPARING = "正在对比商品..."
COMPARE_CLARIFY = "你想对比哪几个商品？可以说「第一个和第二个」，或者从推荐列表中选择。"
COMPARE_INSUFFICIENT = "对比需要至少两个候选商品，先看看推荐再对比吧。"
CART_EMPTY = "购物车是空的，先把商品加入购物车。"

# ── Context-aware clarification analysis ─────────────────────────────

CLARIFICATION_CATEGORY_HINT = (
    "我先看了一下你的需求，目前还不确定你想买哪个品类。"
    "告诉我品类后，推荐会更精准。"
)
CLARIFICATION_BUDGET_HINT_TEMPLATE = (
    "我已经定位到{category}方向，但还需要知道你的预算范围。"
    "不同价位的商品差异比较大，补齐预算后推荐会更稳。"
)

# ── Clarification preference questions (centralised from slot_checker) ─

CLARIFY_PREFERENCE_QUESTION = "请问您更看重哪个方面？"
CLARIFY_SKIN_TYPE_QUESTION = "请问您的肤质更接近哪一种？"
CLARIFY_SPORT_TYPE_QUESTION = "主要用于哪类运动或穿着场景？"

PHONE_PREFERENCE_OPTIONS: tuple[str, ...] = ("拍照", "续航", "性价比", "游戏")
DIGITAL_PREFERENCE_OPTIONS: tuple[str, ...] = ("音质", "续航", "轻便", "性价比")
SKIN_TYPE_OPTIONS: tuple[str, ...] = ("油性", "干性", "混合", "敏感")
SPORT_TYPE_OPTIONS: tuple[str, ...] = ("跑步", "健身", "户外", "日常通勤")

# ── No-match suggestion fragments (centralised from handlers) ────────

NO_MATCH_SUGGEST_RELAX_BUDGET = "放宽预算"
NO_MATCH_SUGGEST_CHANGE_PRODUCT_TYPE = "换一个具体商品类型"
NO_MATCH_SUGGEST_ADD_PRODUCT_TYPE = "补充具体商品类型"
NO_MATCH_SUGGEST_REMOVE_EXCLUSIONS = "去掉部分排除条件"
NO_MATCH_SUGGEST_CHANGE_CATEGORY = "换一个品类"
NO_MATCH_FALLBACK_PRODUCT_TYPE_HINT = "具体商品类型"

# ── Thinking category-aware templates ────────────────────────────────

THINKING_SEARCHING_CATEGORY_TEMPLATE = "正在{category}品类中检索匹配商品..."
THINKING_RANKING_CATEGORY_TEMPLATE = "正在{category}候选中排序和筛选..."
THINKING_DECISION_CATEGORY_TEMPLATE = "正在为你生成{category}适配建议..."
