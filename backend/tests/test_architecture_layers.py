"""Guard the intended API -> Runtime -> Service -> Repo dependency direction."""

from __future__ import annotations

import ast
from pathlib import Path


SRC_ROOT = Path(__file__).resolve().parents[1] / "src"


def test_api_and_runtime_do_not_import_repos_directly():
    offenders = _imports_matching(
        [SRC_ROOT / "api", SRC_ROOT / "runtime"],
        forbidden_prefix="src.repos",
    )

    assert offenders == []


def test_repos_do_not_import_services():
    offenders = _imports_matching([SRC_ROOT / "repos"], forbidden_prefix="src.services")

    assert offenders == []


def _imports_matching(roots: list[Path], forbidden_prefix: str) -> list[str]:
    offenders: list[str] = []
    for root in roots:
        for path in sorted(root.rglob("*.py")):
            tree = ast.parse(path.read_text(encoding="utf-8"))
            for node in ast.walk(tree):
                module = _imported_module(node)
                if module and (module == forbidden_prefix or module.startswith(f"{forbidden_prefix}.")):
                    offenders.append(f"{path.relative_to(SRC_ROOT)} imports {module}")
    return offenders


def _imported_module(node: ast.AST) -> str | None:
    if isinstance(node, ast.ImportFrom):
        return node.module
    if isinstance(node, ast.Import):
        return node.names[0].name if node.names else None
    return None


# ---------- Mock boundary guard (铁律 5) ----------

MOCK_ALLOWED_IO_PREFIXES = {
    "httpx",
    "os",
    "shutil",
    "pathlib",
    "builtins",
    "asyncio.sleep",
    "time",
    "datetime",
}

MOCK_ALLOWED_INTERNAL = {
    "src.runtime.pipeline.run_criteria",
    "src.runtime.pipeline.run_intent",
    "src.runtime.pipeline.run_retrieval",
    "src.runtime.pipeline.run_recommendation_text",
    "src.runtime.pipeline.run_recommendation_text_stream",
    "src.runtime.pipeline.run_decision",
    "src.runtime.pipeline.run_multimodal",
    "src.runtime.pipeline.run_image_embedding",
    "src.runtime.pipeline.HEARTBEAT_INTERVAL_SECONDS",
    "src.runtime.pipeline.register_chat_turn",
    "src.runtime.pipeline.clear_chat_turn",
    "src.runtime.pipeline.record_audit_event",
    "src.runtime.pipeline.get_previous_criteria",
    "src.runtime.pipeline.maybe_intercept_budget_patch",
    "src.runtime.handlers.record_audit_event",
    "src.runtime.handlers.record_retrieval_trace",
    "src.runtime.handlers.record_evidence_links",
    "src.runtime.handlers.save_recommendation_turn",
    "src.runtime.handlers.get_previous_criteria",
    "src.runtime.handlers.get_previous_product_ids",
    "src.runtime.handlers.get_previous_deck_id",
    "src.runtime.handlers.get_feedback_context",
    "src.runtime.handlers.get_evidence",
    "src.runtime.handlers.get_product",
    "src.runtime.streaming.is_chat_turn_cancellation_requested",
}

_MODULE_MAP = {
    "llm_gateway": "src.services.llm_gateway",
    "llm_client": "src.services.llm_client",
    "embedding": "src.services.embedding",
    "reranker": "src.services.reranker",
    "pipeline_module": "src.runtime.pipeline",
    "handlers_module": "src.runtime.handlers",
    "streaming_module": "src.runtime.streaming",
    "retriever": "src.services.retriever",
}

_MOCK_KNOWN_VIOLATIONS = {
    "tests/conftest.py::mock_external_ai -> src.services.llm_gateway._chat_completion",
    "tests/conftest.py::mock_external_ai -> src.services.llm_gateway._chat_completion_stream",
    "tests/conftest.py::mock_external_ai -> src.services.embedding._embedding_request",
    "tests/conftest.py::mock_external_ai -> src.services.embedding._vl_embedding_request",
    "tests/conftest.py::mock_external_ai -> src.services.reranker._rerank_request",
    "tests/conftest.py::_patch_vector_search_for_sqlite_tests -> src.services.retriever.list_vector_chunks_by_similarity",
    "tests/test_retrieval.py::test_retrieve_prefers_pgvector_hits -> src.services.retriever.list_vector_chunks_by_similarity",
    "tests/test_retrieval.py::test_retrieve_returns_empty_without_db_vector_hits -> src.services.retriever.embed_text",
    # Goal 7: LLM bad response defense — stub stage raising RuntimeError
    # to simulate invalid/empty/hallucinated responses (no llm_gateway._chat_completion
    # mock needed — uses MOCK_ALLOWED_INTERNAL stage entry points instead)
    "tests/test_viewmodel_pipeline.py::test_viewmodel_decision_winner_not_in_candidates_locked_to_scoring_winner -> src.services.llm_gateway._chat_completion",
    # Observability tests: mock AsyncSession to verify graceful degradation on DB failures
    "tests/test_observability_llm.py::test_insert_llm_call_returns_none_on_database_error -> src.repos.observability_llm.AsyncSession",
    "tests/test_observability_sse.py::test_insert_sse_event_returns_none_on_database_error -> src.repos.observability_llm.AsyncSession",
    # Demo smoke: mock internal constants and helpers for timeout testing
    "tests/test_demo_smoke.py::test_demo_smoke_run_turn_times_out -> demo_smoke.TURN_TIMEOUT_SECONDS",
    "tests/test_demo_smoke.py::test_demo_smoke_run_turn_times_out -> demo_smoke._collect_turn_events",
    # VL embedding tests: mock internal _vl_embedding_request for payload/error testing
    "tests/test_image_embedding.py::test_vl_embedding_request_sends_correct_payload -> src.services.embedding._vl_embedding_request",
    "tests/test_image_embedding.py::test_vl_embedding_request_omits_parameters_when_no_dimensions -> client.post",
    "tests/test_image_embedding.py::test_vl_embedding_raises_on_empty_response -> src.services.embedding._vl_embedding_request",
    "tests/test_image_embedding.py::test_vl_embedding_raises_on_malformed_embedding -> src.services.embedding._vl_embedding_request",
    # message_rules tests seed known brands for deterministic brand extraction testing
    "tests/test_message_rules.py::_seed_test_brands -> mr.get_known_brands",
    # Pipeline test: add_to_cart reclassification mock
    "tests/test_pipeline.py::test_add_to_cart_no_product_reference_reclassified_to_recommend -> src.runtime.pipeline.get_previous_product_ids",
    # Pipeline test: compare reclassification mock (same pattern as add_to_cart guard)
    "tests/test_pipeline.py::test_compare_no_previous_products_reclassified_to_recommend -> src.runtime.pipeline.get_previous_product_ids",
    "tests/test_pipeline.py::test_compare_single_product_reclassified_to_recommend -> src.runtime.pipeline.get_previous_product_ids",
}


def test_mock_targets_are_at_io_boundary_or_allowlisted():
    violations = _find_mock_violations([Path(__file__).resolve().parent])
    new_violations = [v for v in violations if v not in _MOCK_KNOWN_VIOLATIONS]
    assert new_violations == [], (
        "mock targets must be I/O boundaries or in MOCK_ALLOWED_INTERNAL.\n"
        "Update the allowlist or rewrite the mock. New violations:\n" + "\n".join(new_violations)
    )


def _find_mock_violations(roots: list[Path]) -> list[str]:
    violations: list[str] = []
    for root in roots:
        for path in sorted(root.rglob("*.py")):
            tree = ast.parse(path.read_text(encoding="utf-8"))
            for current_fn, node in _function_calls(tree):
                if not isinstance(node, ast.Call):
                    continue
                if not _is_setattr_call(node):
                    continue
                target = _extract_mock_target(node)
                if target is None:
                    continue
                if _is_mock_allowed(target):
                    continue
                rel = str(path.relative_to(root.parent)).replace("\\", "/")
                violations.append(f"{rel}::{current_fn} -> {target}")
    return violations


def _function_calls(tree: ast.AST) -> list[tuple[str, ast.AST]]:
    calls: list[tuple[str, ast.AST]] = []
    for fn in ast.walk(tree):
        if not isinstance(fn, (ast.FunctionDef, ast.AsyncFunctionDef)):
            continue
        for node in ast.walk(fn):
            calls.append((fn.name, node))
    return calls


def _is_setattr_call(node: ast.Call) -> bool:
    func = node.func
    return (
        isinstance(func, ast.Attribute)
        and func.attr == "setattr"
        and isinstance(func.value, ast.Name)
        and func.value.id == "monkeypatch"
    )


def _extract_mock_target(node: ast.Call) -> str | None:
    if len(node.args) < 2:
        return None
    first, second = node.args[0], node.args[1]
    if isinstance(first, ast.Constant) and isinstance(first.value, str):
        return first.value
    if isinstance(first, ast.Name) and isinstance(second, ast.Constant) and isinstance(second.value, str):
        prefix = _MODULE_MAP.get(first.id, first.id)
        return f"{prefix}.{second.value}"
    return None


def _is_mock_allowed(target: str) -> bool:
    for prefix in MOCK_ALLOWED_IO_PREFIXES:
        if target == prefix or target.startswith(f"{prefix}."):
            return True
    if target in MOCK_ALLOWED_INTERNAL:
        return True
    return False
