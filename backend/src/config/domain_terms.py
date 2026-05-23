"""Shared ecommerce domain terms and Chinese negation helpers."""

from __future__ import annotations


SKIN_TERMS: dict[str, str] = {
    "油皮": "油性",
    "油性": "油性",
    "混油": "混合性",
    "混合": "混合性",
    "干皮": "干性",
    "干性": "干性",
    "敏感": "敏感",
    "中性": "中性",
    "痘肌": "痘肌",
}

CATEGORY_TERMS: dict[str, tuple[str, ...]] = {
    "美妆护肤": ("洗面奶", "防晒", "护肤", "肤", "洁面", "面霜"),
    "数码电子": ("耳机", "手机", "电脑", "数码"),
    "服饰运动": ("跑鞋", "运动", "衣服", "服饰"),
    "食品饮料": ("食品", "饮料", "零食", "无糖"),
}

INTENT_TERMS: dict[str, tuple[str, ...]] = {
    "add_to_cart": ("加到购物车", "加入购物车", "加购", "买这个"),
    "view_cart": ("购物车", "看看车", "查看车"),
    "feedback": ("不喜欢", "不要这个", "换一个", "太贵"),
    "shopping": ("买", "推荐", "预算", "适合"),
}

SCENARIO_TERMS = (
    "日常",
    "通勤",
    "户外",
    "运动",
    "跑步",
    "训练",
    "旅行",
    "露营",
    "送礼",
    "办公",
    "早餐",
    "健身",
)

SCENARIO_VALUES: dict[str, str] = {
    "日常": "日常使用",
    "每天": "日常使用",
    "通勤": "日常使用",
    "户外": "户外防晒",
    "防晒": "户外防晒",
    "送": "送礼",
    "礼物": "送礼",
}

INGREDIENT_TERMS = (
    "酒精",
    "香精",
    "防腐剂",
    "烟酰胺",
    "透明质酸",
    "氨基酸",
    "水杨酸",
    "视黄醇",
    "神经酰胺",
    "二裂酵母",
    "马齿苋",
    "防晒",
    "控油",
    "保湿",
    "修护",
    "抗初老",
)

AVOIDABLE_TRAIT_TERMS = (
    "酒精",
    "香精",
    "防腐剂",
    "水杨酸",
    "视黄醇",
    "小零件",
)

WARNING_MARKERS = (
    "不适合",
    "慎用",
    "不建议",
    "避免",
    "过敏",
    "刺痛",
    "泛红",
    "风险",
    "禁忌",
    "立即停用",
    "暂停使用",
    "不耐受",
    "红肿",
    "瘙痒",
    "闷痘",
)

POSITIVE_MARKERS = ("适合", "主打", "推荐", "清爽", "温和", "性价比", "高倍", "轻量", "稳定", "放心")

NEGATION_PREFIXES = ("无", "不含", "不", "没有", "避免", "非", "抗", "拒绝", "别")
NEGATION_SCOPE_PUNCTUATION = "，。；;,.！!？?\n"
NEGATION_SCOPE_BREAKERS = ("但", "但是", "不过", "然而", "却", "含有", "添加")


def has_negation_prefix(text: str, term: str) -> bool:
    start = 0
    found_negated = False
    while True:
        index = text.find(term, start)
        if index == -1:
            return found_negated
        prefix = term_scope_prefix(text, index)
        neg_positions = [prefix.rfind(neg) for neg in NEGATION_PREFIXES]
        neg_index = max(neg_positions)
        if neg_index >= 0 and not any(breaker in prefix[neg_index:] for breaker in NEGATION_SCOPE_BREAKERS):
            found_negated = True
            start = index + len(term)
            continue
        return False


def term_scope_prefix(text: str, index: int) -> str:
    scope_start = 0
    for punct in NEGATION_SCOPE_PUNCTUATION:
        punct_index = text.rfind(punct, 0, index)
        if punct_index >= 0:
            scope_start = max(scope_start, punct_index + 1)
    return text[scope_start:index]


def extract_terms(text: str, terms: list[str] | tuple[str, ...]) -> list[str]:
    return [term for term in terms if term in text and not has_negation_prefix(text, term)]


def extract_skin_types(text: str) -> list[str]:
    result: list[str] = []
    for token, value in SKIN_TERMS.items():
        if token in text and not has_negation_prefix(text, token) and value not in result:
            result.append(value)
    return result


def contains_any(text: str, terms: tuple[str, ...]) -> bool:
    return any(term in text for term in terms)


def category_from_text(text: str) -> str | None:
    for category, terms in CATEGORY_TERMS.items():
        if contains_any(text, terms):
            return category
    return None


def first_skin_type(text: str) -> str | None:
    matches = extract_skin_types(text)
    return matches[0] if matches else None


def scenario_from_text(text: str, category: str | None = None) -> str | None:
    for token, scenario in SCENARIO_VALUES.items():
        if token in text and not has_negation_prefix(text, token):
            if scenario == "日常使用" and category == "美妆护肤":
                return "日常护肤"
            return scenario
    if category:
        return "日常使用"
    return None


def extract_avoided_traits(text: str, terms: tuple[str, ...] = AVOIDABLE_TRAIT_TERMS) -> list[str]:
    return [term for term in terms if term in text and has_negation_prefix(text, term)]
