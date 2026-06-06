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
    "美妆护肤": (
        "洗面奶",
        "防晒",
        "护肤",
        "肤",
        "洁面",
        "面霜",
        "精华",
        "化妆",
        "卸妆",
        "面膜",
        "粉底",
        "隔离",
        "爽肤",
        "柔肤",
        "神仙水",
        "美白",
        "祛痘",
        "抗老",
        "抗皱",
        "保湿",
        "控油",
        "祛斑",
        "修复",
        "敏感肌",
    ),
    "数码电子": (
        "耳机",
        "手机",
        "电脑",
        "笔记本",
        "平板",
        "数码",
        "拍照",
        "摄影",
        "相机",
        "拍摄",
        "单反",
        "微单",
        "镜头",
        "自拍",
        "存储",
        "屏幕",
        "处理器",
        "芯片",
        "充电器",
        "数据线",
        "移动电源",
        "充电宝",
        "音箱",
        "蓝牙",
        "智能手表",
        "手环",
    ),
    "服饰运动": (
        "跑鞋",
        "运动",
        "衣服",
        "服饰",
        "穿搭",
        "穿",
        "鞋",
        "T恤",
        "卫衣",
        "外套",
        "裤子",
        "裙子",
        "背包",
        "帽",
        "袜子",
        "篮球",
        "足球",
        "瑜伽",
        "跑步",
        "健身",
        "户外",
        "徒步",
        "登山",
        "骑行",
        "游泳",
    ),
    "食品生活": (
        "食品",
        "饮料",
        "零食",
        "无糖",
        "麦片",
        "咖啡",
        "茶饮",
        "调味品",
        "酱油",
        "牛奶",
        "酸奶",
        "果汁",
        "坚果",
        "饼干",
        "巧克力",
        "蛋糕",
        "面包",
        "米",
        "油",
        "盐",
        "糖",
        "酒",
        "啤酒",
        "红酒",
        "纯净水",
        "苏打",
        "碳酸",
        "豆浆",
        "豆奶",
        "燕麦",
        "冲调",
        "方便面",
    ),
}

KNOWN_CATEGORIES = frozenset(CATEGORY_TERMS)

CATEGORY_ALIASES: dict[str, tuple[str, ...]] = {
    "美妆护肤": ("美妆护肤", "美妆", "护肤"),
    "数码电子": ("数码电子", "数码", "电子"),
    "服饰运动": ("服饰运动", "服饰", "运动"),
    "食品生活": ("食品生活", "食品饮料", "食品", "饮料", "零食", "调味品"),
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
    "跑步鞋": ("跑步鞋", "跑鞋", "运动鞋", "训练鞋", "慢跑鞋"),
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

# Parent→children hierarchy: when a user queries a broad term (e.g. "裤子"),
# expand to all child canonical types so the SQL IN filter catches subcategories.
# Children MUST be canonical keys in PRODUCT_TYPE_ALIASES.
PRODUCT_TYPE_HIERARCHY: dict[str, tuple[str, ...]] = {
    "裤子": ("户外裤", "运动长裤", "运动短裤", "瑜伽裤"),
    "上衣": ("短袖T恤", "速干T恤", "卫衣"),
    "鞋": ("跑步鞋", "篮球鞋", "徒步鞋"),
    "耳机": ("真无线耳机",),
    "洁面": ("洁面",),
    "饮品": ("茶饮", "碳酸饮料", "功能饮料", "咖啡", "牛奶", "酸奶"),
    "零食": ("坚果/零食",),
    "电脑": ("笔记本电脑", "平板电脑"),
    "手机": ("智能手机",),
}

PRODUCT_TYPE_TO_CATEGORY: dict[str, str] = {
    "洁面": "美妆护肤",
    "防晒": "美妆护肤",
    "面霜": "美妆护肤",
    "精华": "美妆护肤",
    "化妆水": "美妆护肤",
    "卸妆": "美妆护肤",
    "粉底液": "美妆护肤",
    "面膜": "美妆护肤",
    "眉笔": "美妆护肤",
    "蜜粉": "美妆护肤",
    "唇釉": "美妆护肤",
    "眼霜": "美妆护肤",
    "智能手机": "数码电子",
    "真无线耳机": "数码电子",
    "笔记本电脑": "数码电子",
    "平板电脑": "数码电子",
    "跑步鞋": "服饰运动",
    "篮球鞋": "服饰运动",
    "短袖T恤": "服饰运动",
    "速干T恤": "服饰运动",
    "运动长裤": "服饰运动",
    "运动短裤": "服饰运动",
    "卫衣": "服饰运动",
    "徒步鞋": "服饰运动",
    "户外裤": "服饰运动",
    "背包": "服饰运动",
    "帽子": "服饰运动",
    "瑜伽裤": "服饰运动",
    "茶饮": "食品生活",
    "碳酸饮料": "食品生活",
    "功能饮料": "食品生活",
    "坚果/零食": "食品生活",
    "方便食品": "食品生活",
    "咖啡": "食品生活",
    "牛奶": "食品生活",
    "酸奶": "食品生活",
    "调味品": "食品生活",
}

_PRODUCT_TYPE_INDEX = {
    alias.casefold(): canonical
    for canonical, aliases in PRODUCT_TYPE_ALIASES.items()
    for alias in (canonical, *aliases)
}

INTENT_TERMS: dict[str, tuple[str, ...]] = {
    "add_to_cart": ("加到购物车", "加入购物车", "加购", "买这个"),
    "view_cart": ("购物车", "看看车", "查看车"),
    "checkout_preview": ("就买这个", "买它", "下单", "确认购买", "确认下单"),
    "checkout_confirm": ("确认", "确认了", "就这样", "就这样吧"),
    "checkout_cancel": ("取消购买", "算了不买", "不买了"),
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


def normalize_category(value: object) -> str | None:
    if not isinstance(value, str):
        return None
    key = value.strip()
    if not key:
        return None
    if key.casefold() in {"null", "none", "nil", "n/a", "na", "unknown", "undefined"}:
        return None
    if key in {"无", "未知", "未识别"}:
        return None
    lowered = key.casefold()
    for category in CATEGORY_TERMS:
        if lowered == category.casefold():
            return category
    for category, aliases in CATEGORY_ALIASES.items():
        if any(lowered == alias.casefold() for alias in aliases):
            return category
    for category, aliases in CATEGORY_ALIASES.items():
        if category.casefold() in lowered:
            return category
        if any(len(alias) >= 2 and alias.casefold() in lowered for alias in aliases):
            return category
    return key


def normalize_product_type(value: object) -> str | None:
    if value is None:
        return None
    if isinstance(value, int | float):
        value = str(value)
    if not isinstance(value, str):
        return None
    key = value.strip()
    if not key:
        return None
    if key.casefold() in {"null", "none", "nil", "n/a", "na", "unknown", "undefined"}:
        return None
    if key in {"无", "未知", "未识别"}:
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
    result = {canonical, *PRODUCT_TYPE_ALIASES.get(canonical, ())}
    # Expand hierarchy: if this type is a parent, include all children's aliases
    children = PRODUCT_TYPE_HIERARCHY.get(canonical, ())
    for child in children:
        result.add(child)
        result.update(PRODUCT_TYPE_ALIASES.get(child, ()))
    return tuple(result)


def infer_category_from_product_type(value: str | None) -> str | None:
    canonical = normalize_product_type(value)
    if not canonical:
        return None
    direct = PRODUCT_TYPE_TO_CATEGORY.get(canonical)
    if direct:
        return direct
    # Hierarchy fallback: "裤子" is a parent, not a canonical key, so check children
    children = PRODUCT_TYPE_HIERARCHY.get(canonical, ())
    for child in children:
        cat = PRODUCT_TYPE_TO_CATEGORY.get(child)
        if cat:
            return cat
    return None


def normalize_product_type_for_category(
    value: object,
    category: object,
    *,
    allow_unknown: bool = False,
) -> str | None:
    canonical = normalize_product_type(value)
    if not canonical:
        return None
    normalized_category = normalize_category(category)
    inferred_category = infer_category_from_product_type(canonical)
    if not inferred_category and _is_exact_category_label(canonical):
        return None
    if not inferred_category:
        return canonical if allow_unknown else None
    if normalized_category and inferred_category and normalized_category != inferred_category:
        return None
    return canonical


def _is_exact_category_label(value: str) -> bool:
    key = value.strip().casefold()
    if not key:
        return False
    if any(key == category.casefold() for category in KNOWN_CATEGORIES):
        return True
    return any(key == alias.casefold() for aliases in CATEGORY_ALIASES.values() for alias in aliases)


def is_supported_product_type(value: str | None) -> bool:
    canonical = normalize_product_type(value)
    if not canonical:
        return False
    if canonical in PRODUCT_TYPE_ALIASES:
        return True
    # Hierarchy parents (裤子, 上衣, 鞋, 手机...) are also supported
    if canonical in PRODUCT_TYPE_HIERARCHY:
        return True
    return False


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


def avoid_trait_aliases(token: str) -> tuple[str, ...]:
    normalized = token.strip()
    if not normalized:
        return ()
    aliases = AVOID_TRAIT_MATCH_TERMS.get(normalized.lower()) or AVOID_TRAIT_MATCH_TERMS.get(normalized)
    return aliases or (normalized,)


# ── Data-driven product indices (lazy, built from actual product data) ──────

_brand_set: frozenset[str] | None = None
_brand_case_map: dict[str, str] = {}  # lowercased → original-case catalog brand name
_sub_category_set: frozenset[str] | None = None


def _ensure_data_indices() -> None:
    global _brand_set, _brand_case_map, _sub_category_set
    if _brand_set is not None:
        return
    from src.repos.products import load_raw_products  # lazy to avoid circular import

    raw = load_raw_products()
    _brand_set = frozenset(
        p.get("brand", "").strip().lower() for p in raw if p.get("brand") and p.get("brand", "").strip()
    )
    _brand_case_map = {
        p.get("brand", "").strip().lower(): p.get("brand", "").strip()
        for p in raw
        if p.get("brand") and p.get("brand", "").strip()
    }
    _sub_category_set = frozenset(
        p.get("sub_category", "").strip() for p in raw if p.get("sub_category") and p.get("sub_category", "").strip()
    )


def get_known_brands() -> frozenset[str]:
    _ensure_data_indices()
    return _brand_set  # type: ignore[return-value]


def get_brand_case(lowered: str) -> str:
    """Return the original-case catalog brand name for a lowercased brand."""
    _ensure_data_indices()
    return _brand_case_map.get(lowered, lowered)


def get_known_sub_categories() -> frozenset[str]:
    _ensure_data_indices()
    return _sub_category_set  # type: ignore[return-value]


# ── Brand synonym mapping ──────────────────────────────────────────────
# Maps user expressions (lowercased) → catalog brand name (original case).
# Catalog-internal brand aliases can be extracted from product RAG knowledge
# chunks; manual entries fill gaps for common expressions not in the catalog.

BRAND_SYNONYMS: dict[str, str] = {
    # Apple products — users say "Mac"/"iPhone" but catalog uses "Apple 苹果"
    "mac": "Apple 苹果",
    "macbook": "Apple 苹果",
    "iphone": "Apple 苹果",
    "airpods": "Apple 苹果",
    "ipad": "Apple 苹果",
    "apple": "Apple 苹果",
    "苹果": "Apple 苹果",
    # Common English→Chinese brand mappings
    "huawei": "华为",
    "nike": "Nike",
    "adidas": "阿迪达斯",
    "arc'teryx": "始祖鸟",
}
# catalog name → self (allows is_known_brand_or_synonym to unify both paths)
_SELF_SYNONYMS: dict[str, str] = {}


def _ensure_synonym_indices() -> None:
    """Build self-mapping for every catalog brand so is_known_brand_or_synonym
    works uniformly for both catalog brands and user synonyms."""
    if _SELF_SYNONYMS:
        return
    _ensure_data_indices()
    for brand_lower in _brand_set:  # type: ignore[union-attr]
        original = _brand_case_map.get(brand_lower, brand_lower)
        _SELF_SYNONYMS[brand_lower] = original


def is_known_brand_or_synonym(value: str) -> bool:
    """Check if `value` is a known brand (catalog or user synonym)."""
    _ensure_synonym_indices()
    key = value.strip().lower()
    if not key or len(key) < 2:
        return False
    return key in _SELF_SYNONYMS or key in BRAND_SYNONYMS


def resolve_brand_prefer(value: str) -> str | None:
    """Resolve a user-supplied term to a catalog brand name (original case).

    Returns the catalog brand name or None when the term is not a known brand.
    """
    _ensure_synonym_indices()
    key = value.strip().lower()
    if not key or len(key) < 2:
        return None
    if key in BRAND_SYNONYMS:
        return BRAND_SYNONYMS[key]
    if key in _SELF_SYNONYMS:
        return _SELF_SYNONYMS[key]
    return None
