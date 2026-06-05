"""Deterministic recommendation reason atoms.

Reason atoms are stored-product facts selected for the current criteria. They
keep product cards auditable while giving the LLM safe material to polish in
long-form recommendation text.
"""

from __future__ import annotations

from src.config.domain_terms import normalize_product_type
from src.repos.documents import ChunkDocument, risk_chunks_for_products
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload, ReasonAtomPayload

_RISK_NOTE_LIMIT = 3
_RISK_NOTE_MAX_CHARS = 180

_REASON_ATOM_LIMIT = 4


def build_reason_atoms(
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload] | None = None,
) -> list[ReasonAtomPayload]:
    evidence = evidence or []
    atoms: list[ReasonAtomPayload] = []

    _append_skin_atom(atoms, criteria, product, evidence)
    _append_product_type_atom(atoms, criteria, product, evidence)
    _append_budget_atom(atoms, criteria, product)
    _append_scenario_atom(atoms, criteria, product, evidence)
    _append_preferred_ingredient_atom(atoms, criteria, product, evidence)
    _append_avoidance_atom(atoms, criteria, product, evidence)
    _append_storage_atom(atoms, criteria, product, evidence)
    _append_dietary_atom(atoms, criteria, product, evidence)

    if not atoms:
        atoms.append(
            ReasonAtomPayload(
                dimension="category",
                value=product.category,
                text=f"{product.category}类目匹配",
                evidence_id="product_field:category",
            )
        )
    return atoms[:_REASON_ATOM_LIMIT]


def reason_from_atoms(product: ProductPayload, atoms: list[ReasonAtomPayload]) -> str:
    if not atoms:
        return f"{product.category}下综合匹配度较高。"
    return "，".join(atom.text for atom in atoms[:2]) + "。"


def _append_skin_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    skin_matches = _unique(product.skin_type_match)
    if not skin_matches:
        return
    requested = criteria.constraints.skin_type
    if requested and requested in skin_matches:
        related = [item for item in skin_matches if item != requested][:1]
        text = f"{requested}肤质匹配" + (f"，兼顾{related[0]}" if related else "")
        value = "、".join([requested, *related])
    else:
        displayed = skin_matches[:2]
        suffix = "等" if len(skin_matches) > 2 else ""
        text = f"{'、'.join(displayed)}{suffix}适用"
        value = "、".join(displayed)
    atoms.append(
        ReasonAtomPayload(
            dimension="skin_type",
            value=value,
            text=text,
            evidence_id=_matching_evidence_id(evidence, skin_matches) or "product_field:skin_type_match",
        )
    )


def _append_product_type_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    product_type = normalize_product_type(product.sub_category)
    requested = normalize_product_type(criteria.constraints.product_type)
    if not product_type:
        return
    if requested and product_type != requested:
        return
    atoms.append(
        ReasonAtomPayload(
            dimension="product_type",
            value=product_type,
            text=f"{product_type}品类匹配",
            evidence_id=_matching_evidence_id(evidence, [product_type, product.sub_category or ""])
            or "product_field:sub_category",
        )
    )


def _append_budget_atom(atoms: list[ReasonAtomPayload], criteria: CriteriaPayload, product: ProductPayload) -> None:
    if product.price is None:
        return
    budget_max = criteria.constraints.budget_max
    if budget_max is None:
        return
    if product.price > budget_max:
        return
    atoms.append(
        ReasonAtomPayload(
            dimension="budget",
            value=f"{product.price:g}",
            text=f"{product.price:g}元符合{budget_max:g}元预算",
            evidence_id="product_field:price",
        )
    )


def _append_scenario_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    requested = criteria.constraints.use_scenario
    scenario = product.use_scenario
    if not requested or not scenario:
        return
    if requested not in scenario and scenario not in requested:
        return
    atoms.append(
        ReasonAtomPayload(
            dimension="use_scenario",
            value=scenario,
            text=f"适合{requested}",
            evidence_id=_matching_evidence_id(evidence, [requested, scenario]) or "product_field:use_scenario",
        )
    )


def _append_preferred_ingredient_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    matches = [item for item in criteria.constraints.ingredient_prefer if item in product.ingredient_tags]
    if not matches:
        return
    displayed = matches[:2]
    atoms.append(
        ReasonAtomPayload(
            dimension="ingredient_prefer",
            value="、".join(displayed),
            text=f"含{'、'.join(displayed)}",
            evidence_id=_matching_evidence_id(evidence, displayed) or "product_field:ingredient_tags",
        )
    )


def _append_avoidance_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    avoid_terms = criteria.constraints.ingredient_avoid
    if not avoid_terms:
        return
    haystack = _fact_haystack(product, evidence)
    cleared = [term for term in avoid_terms if term not in haystack]
    if not cleared:
        return
    displayed = cleared[:2]
    atoms.append(
        ReasonAtomPayload(
            dimension="ingredient_avoid",
            value="、".join(displayed),
            text=f"已避开{'、'.join(displayed)}",
            evidence_id="criteria_filter:ingredient_avoid",
        )
    )


def _append_storage_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    storage = criteria.constraints.storage
    if not storage:
        return
    if storage.casefold() not in _fact_haystack(product, evidence).casefold():
        return
    atoms.append(
        ReasonAtomPayload(
            dimension="storage",
            value=storage,
            text=f"{storage}存储匹配",
            evidence_id=_matching_evidence_id(evidence, [storage]) or "product_field:name",
        )
    )


def _append_dietary_atom(
    atoms: list[ReasonAtomPayload],
    criteria: CriteriaPayload,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> None:
    if not criteria.constraints.dietary:
        return
    haystack = _fact_haystack(product, evidence)
    matches = [item for item in criteria.constraints.dietary if item in haystack]
    if not matches:
        return
    displayed = matches[:2]
    atoms.append(
        ReasonAtomPayload(
            dimension="dietary",
            value="、".join(displayed),
            text=f"{'、'.join(displayed)}需求匹配",
            evidence_id=_matching_evidence_id(evidence, displayed) or "product_field:name",
        )
    )


def _matching_evidence_id(evidence: list[EvidencePayload], tokens: list[str]) -> str | None:
    cleaned = [token for token in tokens if token]
    for item in evidence:
        if any(token in item.snippet for token in cleaned):
            return item.source_id
    return evidence[0].source_id if evidence else None


def _fact_haystack(product: ProductPayload, evidence: list[EvidencePayload]) -> str:
    return " ".join(
        [
            product.name,
            product.category,
            product.sub_category or "",
            product.brand or "",
            product.use_scenario or "",
            *product.skin_type_match,
            *product.ingredient_tags,
            *product.ingredient_avoid,
            *(item.snippet for item in evidence),
        ]
    )


def _unique(items: list[str]) -> list[str]:
    return list(dict.fromkeys(item for item in items if item))


async def fetch_risk_notes_for_products(product_ids: list[str]) -> dict[str, list[str]]:
    """Fetch risk chunks from DB and map to per-product risk note strings."""
    chunks_map = await risk_chunks_for_products(product_ids)
    return {pid: build_risk_notes(chunks) for pid, chunks in chunks_map.items()}


def build_risk_notes(chunks: list[ChunkDocument]) -> list[str]:
    """Map risk-role chunks to concise human-readable risk notes."""
    notes: list[str] = []
    seen: set[str] = set()
    for chunk in chunks:
        chunk_type = chunk.metadata.get("chunk_type", "")
        text = chunk.chunk_text.strip()
        if not text:
            continue

        if chunk_type == "negative_review":
            nickname = chunk.metadata.get("nickname", "")
            rating = chunk.metadata.get("rating", "?")
            note = f"用户{nickname}评分{rating}：{_truncate(text, _RISK_NOTE_MAX_CHARS)}"
        elif chunk_type == "warning":
            note = _truncate(text, _RISK_NOTE_MAX_CHARS)
        elif chunk_type == "faq":
            note = _truncate(text, _RISK_NOTE_MAX_CHARS)
        else:
            note = _truncate(text, _RISK_NOTE_MAX_CHARS)

        if note and note not in seen:
            seen.add(note)
            notes.append(note)
        if len(notes) >= _RISK_NOTE_LIMIT:
            break
    return notes


def _truncate(text: str, max_chars: int) -> str:
    cleaned = " ".join(text.split())
    if len(cleaned) <= max_chars:
        return cleaned
    return cleaned[: max_chars - 1] + "…"
