"""LLM-as-Judge evaluation metrics.

Uses the project's existing Qwen-Plus model (via llm_client internals) as a
reference-free judge for faithfulness, context quality, constraint satisfaction,
hallucination detection, and multi-turn consistency.

No additional API keys or cloud services required — the judge reuses the
project's already-configured Bailian Qwen-Plus profile.
"""

from __future__ import annotations

import json
import re
from typing import Any

from src.services.llm_client import _call_chat_task


async def evaluate_faithfulness(answer: str, contexts: list[str]) -> dict[str, float]:
    """RAG faithfulness: are claims in the answer supported by retrieved context?

    Returns dict with 'score' (0-1) and 'unsupported_claims' (count).
    """
    if not answer.strip() or not contexts:
        return {"score": 1.0, "unsupported_claims": 0}

    ctx_text = "\n\n---\n\n".join(f"[{i + 1}] {c}" for i, c in enumerate(contexts))
    prompt = _build_judge_prompt(
        "faithfulness",
        answer=answer,
        contexts=ctx_text,
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "faithfulness_score", "unsupported_claims")


async def evaluate_context_precision(question: str, contexts: list[str]) -> dict[str, float]:
    """Context Precision: fraction of retrieved chunks relevant to the query."""
    if not contexts:
        return {"score": 0.0, "relevant_count": 0}

    ctx_text = "\n\n---\n\n".join(f"[{i + 1}] {c}" for i, c in enumerate(contexts))
    prompt = _build_judge_prompt(
        "context_precision",
        question=question,
        contexts=ctx_text,
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "context_precision_score", "relevant_count")


async def evaluate_context_recall(answer: str, contexts: list[str]) -> dict[str, float]:
    """Context Recall: does the retrieved context cover all info needed in the answer?"""
    if not answer.strip() or not contexts:
        return {"score": 0.0, "covered_count": 0}

    ctx_text = "\n\n---\n\n".join(f"[{i + 1}] {c}" for i, c in enumerate(contexts))
    prompt = _build_judge_prompt(
        "context_recall",
        answer=answer,
        contexts=ctx_text,
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "context_recall_score", "covered_count")


async def evaluate_answer_correctness(question: str, answer: str, contexts: list[str]) -> dict[str, float]:
    """Answer Correctness: does the answer factually address the question?"""
    if not answer.strip():
        return {"score": 0.0, "error_count": 1}

    ctx_text = "\n\n---\n\n".join(f"[{i + 1}] {c}" for i, c in enumerate(contexts))
    prompt = _build_judge_prompt(
        "answer_correctness",
        question=question,
        answer=answer,
        contexts=ctx_text,
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "correctness_score", "error_count")


async def evaluate_constraint_satisfaction(
    answer: str,
    recommended_products: list[dict[str, Any]],
    constraints: dict[str, Any],
) -> dict[str, float]:
    """LLM-judged constraint satisfaction: do recommendations meet user's hard constraints?"""
    if not recommended_products:
        return {"score": 0.0, "violations": 0}

    prompt = _build_judge_prompt(
        "constraint_satisfaction",
        constraints=json.dumps(constraints, ensure_ascii=False),
        products=json.dumps(
            [
                {
                    "name": p.get("name", ""),
                    "price": p.get("price"),
                    "category": p.get("category", ""),
                    "metadata": p.get("metadata", {}),
                }
                for p in recommended_products
            ],
            ensure_ascii=False,
        ),
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "constraint_satisfaction_score", "violations")


async def evaluate_hallucination_rate(answer: str, contexts: list[str]) -> dict[str, float]:
    """Hallucination rate: fraction of answer claims unsupported by retrieved context."""
    faithfulness_result = await evaluate_faithfulness(answer, contexts)
    return {
        "hallucination_rate": 1.0 - faithfulness_result["score"],
        "unsupported_claims": faithfulness_result["unsupported_claims"],
    }


async def evaluate_multi_turn_consistency(
    conversation_history: list[dict[str, str]],
    final_answer: str,
    final_contexts: list[str],
) -> dict[str, float]:
    """Multi-turn consistency: does the final answer respect all previous constraints?"""
    if not conversation_history:
        return {"score": 1.0}

    history_text = "\n".join(f"{t['role']}: {t['content']}" for t in conversation_history)
    ctx_text = "\n\n---\n\n".join(f"[{i + 1}] {c}" for i, c in enumerate(final_contexts))
    prompt = _build_judge_prompt(
        "multi_turn_consistency",
        history=history_text,
        answer=final_answer,
        contexts=ctx_text,
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "consistency_score", "inconsistencies")


async def evaluate_ranking_reasonableness(
    question: str,
    ranked_products: list[dict[str, Any]],
    constraints: dict[str, Any],
) -> dict[str, float]:
    """Ranking reasonableness: are top products ordered by constraint match quality?"""
    if len(ranked_products) <= 1:
        return {"score": 1.0}

    prompt = _build_judge_prompt(
        "ranking_reasonableness",
        question=question,
        constraints=json.dumps(constraints, ensure_ascii=False),
        products=json.dumps(
            [
                {"rank": i + 1, "name": p.get("name", ""), "price": p.get("price")}
                for i, p in enumerate(ranked_products[:5])
            ],
            ensure_ascii=False,
        ),
    )
    result = await _call_judge(prompt)
    return _parse_score_and_count(result, "ranking_score", "misplacements")


# ─── helpers ──────────────────────────────────────────────────────────────────

_JUDGE_PROMPTS: dict[str, str] = {
    "faithfulness": """你是中文电商 RAG 评测法官。你的任务是判断"答案"中的每一条事实主张是否能在"检索上下文"中找到支撑证据。

检索上下文:
{contexts}

答案:
{answer}

请输出 JSON，字段为:
- "faithfulness_score": 0.0-1.0，表示有上下文支撑的 claims 比例
- "unsupported_claims": 无法在上下文中找到证据的 claims 数量（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
    "context_precision": """你是中文电商 RAG 评测法官。你的任务是判断"检索上下文"中的每个 chunk 是否与用户问题相关。

用户问题:
{question}

检索上下文:
{contexts}

请输出 JSON，字段为:
- "context_precision_score": 0.0-1.0，表示相关的 chunk 比例
- "relevant_count": 相关的 chunk 数量（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
    "context_recall": """你是中文电商 RAG 评测法官。你的任务是判断"答案"中需要的信息是否都能在"检索上下文"中找到。

答案:
{answer}

检索上下文:
{contexts}

请输出 JSON，字段为:
- "context_recall_score": 0.0-1.0，表示答案所需信息被上下文覆盖的比例
- "covered_count": 被覆盖的信息点数量（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
    "answer_correctness": """你是中文电商 RAG 评测法官。你的任务是判断"答案"是否准确、有用地回答了用户问题，且没有编造商品、价格、优惠信息。

用户问题:
{question}

答案:
{answer}

检索上下文:
{contexts}

请输出 JSON，字段为:
- "correctness_score": 0.0-1.0，表示答案的事实正确性
- "error_count": 答案中的事实错误数量（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
    "constraint_satisfaction": """你是中文电商 RAG 评测法官。你的任务是检查"推荐商品"是否满足"用户约束"。

用户约束:
{constraints}

推荐商品:
{products}

请逐条检查每个商品的每个约束是否满足，输出 JSON:
- "constraint_satisfaction_score": 0.0-1.0，满足的约束比例
- "violations": 违反约束的次数（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
    "multi_turn_consistency": """你是中文电商 RAG 评测法官。你的任务是判断"最终答案"是否与"对话历史"中已建立的约束保持一致，没有遗忘或矛盾。

对话历史:
{history}

最终答案:
{answer}

检索上下文:
{contexts}

请输出 JSON，字段为:
- "consistency_score": 0.0-1.0，表示一致性程度
- "inconsistencies": 不一致的地方数量（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
    "ranking_reasonableness": """你是中文电商 RAG 评测法官。你的任务是判断"推荐商品列表"的排序是否合理，即排名靠前的商品是否确实比排名靠后的更符合用户需求。

用户问题:
{question}

用户约束:
{constraints}

排序商品列表:
{products}

请输出 JSON，字段为:
- "ranking_score": 0.0-1.0，表示排序合理性
- "misplacements": 排序不当的商品数量（整数）
- "reasoning": 简短中文理由

只输出 JSON，不要其他内容。""",
}


def _build_judge_prompt(judge_type: str, **kwargs: str) -> str:
    template = _JUDGE_PROMPTS.get(judge_type, "")
    if not template:
        raise ValueError(f"Unknown judge type: {judge_type}")
    return template.format(**kwargs)


async def _call_judge(prompt: str) -> dict[str, Any]:
    """Call the Qwen-Plus model as a judge via the existing llm_client infrastructure."""
    raw = await _call_chat_task(
        "generate_recommendation",  # reuse qwen_plus profile via an existing task
        [{"role": "user", "content": prompt}],
        json_object=True,
    )
    return _parse_json(raw) if raw else {}


def _parse_json(text: str) -> dict[str, Any]:
    try:
        parsed = json.loads(text)
        return parsed if isinstance(parsed, dict) else {}
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, flags=re.S)
        if not match:
            return {}
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            return {}


def _parse_score_and_count(result: dict[str, Any], score_key: str, count_key: str) -> dict[str, float]:
    score = result.get(score_key, 0)
    count = result.get(count_key, 0)
    return {
        "score": float(score) if isinstance(score, (int, float)) else 0.0,
        count_key: int(count) if isinstance(count, (int, float)) else 0,
    }
