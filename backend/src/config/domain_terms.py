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
    "数码电子": ("耳机", "手机", "电脑", "笔记本", "平板", "数码"),
    "服饰运动": ("跑鞋", "运动", "衣服", "服饰"),
    "食品饮料": ("食品", "饮料", "零食", "无糖", "麦片", "咖啡", "茶饮"),
}

PRODUCT_TYPE_ALIASES: dict[str, tuple[str, ...]] = {
    "洁面": ("洁面", "洁面乳", "洗面奶", "洗颜", "泡沫洁面"),
    "防晒": ("防晒", "防晒霜", "防晒乳", "隔离露"),
    "面霜": ("面霜", "乳霜", "修复霜", "特护霜"),
    "精华": ("精华", "精华液", "肌底液"),
    "化妆水": ("化妆水", "爽肤水", "柔肤水", "神仙水"),
    "卸妆": ("卸妆", "卸妆油", "卸妆液"),
    "粉底液": ("粉底液", "粉底"),
    "面膜": ("面膜", "涂抹面膜"),
    "眉笔": ("眉笔",),
    "蜜粉": ("蜜粉", "散粉", "粉饼"),
    "唇釉": ("唇釉",),
    "眼霜": ("眼霜",),
    "智能手机": ("智能手机", "手机", "5g手机", "游戏手机", "旗舰手机"),
    "真无线耳机": ("真无线耳机", "蓝牙耳机", "无线耳机", "耳机", "降噪耳机", "airpods"),
    "笔记本电脑": ("笔记本电脑", "笔记本", "电脑", "轻薄本", "商务本"),
    "平板电脑": ("平板电脑", "平板", "pad", "ipad"),
    "跑步鞋": ("跑步鞋", "跑鞋", "训练鞋", "慢跑鞋"),
    "篮球鞋": ("篮球鞋", "实战篮球鞋"),
    "短袖T恤": ("短袖t恤", "短袖", "t恤", "运动上衣"),
    "速干T恤": ("速干t恤", "速干短袖", "速干上衣"),
    "运动长裤": ("运动长裤", "长裤", "束脚裤"),
    "运动短裤": ("运动短裤", "短裤", "训练裤"),
    "卫衣": ("卫衣", "连帽卫衣"),
    "徒步鞋": ("徒步鞋", "登山鞋"),
    "户外裤": ("户外裤", "软壳长裤"),
    "背包": ("背包", "双肩包", "通勤包"),
    "帽子": ("帽子", "棒球帽", "鸭舌帽", "遮阳帽"),
    "瑜伽裤": ("瑜伽裤", "紧身裤"),
    "茶饮": ("茶饮", "茶饮料", "无糖茶", "乌龙茶", "茉莉花茶"),
    "碳酸饮料": ("碳酸饮料", "气泡水", "苏打水", "汽水", "可乐"),
    "功能饮料": ("功能饮料", "能量饮料", "维生素饮料"),
    "坚果/零食": ("坚果/零食", "坚果", "零食", "点心", "肉松饼", "每日坚果"),
    "方便食品": ("方便食品", "方便面", "泡面", "速食"),
    "咖啡": ("咖啡", "速溶咖啡", "黑咖啡", "冷萃"),
    "牛奶": ("牛奶", "纯牛奶"),
    "酸奶": ("酸奶", "风味酸奶"),
    "调味品": ("调味品", "酱油", "生抽", "老抽"),
}

_PRODUCT_TYPE_INDEX = {
    alias.casefold(): canonical
    for canonical, aliases in PRODUCT_TYPE_ALIASES.items()
    for alias in (canonical, *aliases)
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

FEEDBACK_AVOID_TRAIT_TERMS = (
    *AVOIDABLE_TRAIT_TERMS,
    "耐克",
    "Nike",
    "日系品牌",
    "日系",
    "日本品牌",
    "日本",
)

NEGATIVE_FEEDBACK_ACTIONS = {"not_interested", "dislike", "avoid", "negative"}
NEGATIVE_FEEDBACK_MARKERS = ("不要", "不含", "避免", "除了", "不喜欢", "别再", "不要再")

AVOID_TRAIT_MATCH_TERMS: dict[str, tuple[str, ...]] = {
    "日系": ("SK-II", "资生堂", "安热沙", "珊珂", "芳珂", "优衣库"),
    "日系品牌": ("SK-II", "资生堂", "安热沙", "珊珂", "芳珂", "优衣库"),
    "日本": ("SK-II", "资生堂", "安热沙", "珊珂", "芳珂", "优衣库"),
    "日本品牌": ("SK-II", "资生堂", "安热沙", "珊珂", "芳珂", "优衣库"),
    "nike": ("Nike", "耐克"),
    "耐克": ("Nike", "耐克"),
}

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


def contains_any_casefold(text: str, terms: tuple[str, ...]) -> bool:
    lowered = text.lower()
    return any(term.lower() in lowered for term in terms)


def category_from_text(text: str) -> str | None:
    for category, terms in CATEGORY_TERMS.items():
        if contains_any(text, terms):
            return category
    return None


def normalize_product_type(value: str | None) -> str | None:
    if value is None:
        return None
    key = value.strip()
    if not key:
        return None
    direct = _PRODUCT_TYPE_INDEX.get(key.casefold())
    if direct:
        return direct
    for alias, canonical in _PRODUCT_TYPE_INDEX.items():
        if len(alias) >= 2 and alias in key.casefold():
            return canonical
    return key


def product_type_aliases(value: str | None) -> tuple[str, ...]:
    canonical = normalize_product_type(value)
    if not canonical:
        return ()
    return (canonical, *PRODUCT_TYPE_ALIASES.get(canonical, ()))


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


def is_negative_feedback(action: str, reason: str | None = None) -> bool:
    return action in NEGATIVE_FEEDBACK_ACTIONS or bool(reason and contains_any(reason, NEGATIVE_FEEDBACK_MARKERS))


def extract_feedback_avoid_terms(reason: str) -> list[str]:
    terms = [term for term in FEEDBACK_AVOID_TRAIT_TERMS if term in reason]
    if terms:
        return [term for term in terms if not any(term != other and term in other for other in terms)]

    cleaned = reason
    for marker in NEGATIVE_FEEDBACK_MARKERS:
        cleaned = cleaned.replace(marker, "")
    for filler in ("的", "这个", "这款", "商品", "产品", "品牌", "牌子", "还有什么", "还有吗"):
        cleaned = cleaned.replace(filler, "")
    cleaned = cleaned.strip(" ，。,.！!？?")
    return [cleaned] if cleaned else [reason]


def avoid_trait_matches_text(token: str, text: str) -> bool:
    normalized = token.strip()
    if not normalized:
        return False
    aliases = AVOID_TRAIT_MATCH_TERMS.get(normalized.lower()) or AVOID_TRAIT_MATCH_TERMS.get(normalized)
    if aliases:
        return contains_any_casefold(text, aliases)
    return normalized.lower() in text.lower()
