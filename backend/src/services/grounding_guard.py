"""Output-level anti-hallucination guard for LLM-generated recommendation text.

Validates that any concrete price figure mentioned by the LLM appears in the
actual product database or user budget constraints. If not, returns a
correction notice that should be emitted to the client before the criteria card.

This is a *post-stream* guard — by the time it runs, text has already been
streamed to the client. It provides corrective annotation, not pre-emission
blocking.

Only detects CNY-denominated price mentions (元/块/RMB). Foreign currency
symbols (USD/HKD/JPY) and symbols (¥/$) are not matched by the price regex.
"""

from __future__ import annotations

import re

from src.types.sse_events import ProductPayload


def validate_price_claims(
    text: str,
    products: list[ProductPayload],
    budget_max: float | None = None,
    budget_min: float | None = None,
) -> str | None:
    """Scan recommendation text for price figures and verify each against DB.

    Builds a whitelist of allowed price values from:
      - product base_price
      - each SKU option price
      - user budget bounds

    Returns None if every price figure is valid, or a correction message
    string if any price is not in the whitelist.
    """
    allowed: set[int] = set()

    for p in products:
        if p.price is not None:
            allowed.add(round(p.price))
        if p.sku_options:
            for sku in p.sku_options:
                if isinstance(sku, dict) and "price" in sku:
                    try:
                        allowed.add(round(float(sku["price"])))
                    except (ValueError, TypeError):
                        pass

    if budget_max is not None:
        allowed.add(round(budget_max))
    if budget_min is not None:
        allowed.add(round(budget_min))

    violations: list[str] = []
    for match in re.finditer(r"(\d+(?:\.\d+)?)\s*(?:元|块|RMB)", text):
        val = round(float(match.group(1)))
        if val not in allowed:
            violations.append(match.group(0))

    if violations:
        return (
            f"（已自动修正上文中的价格信息：{', '.join(violations)} "
            f"未在商品库中记录，请以商品卡片标注的价格为准）"
        )
    return None
