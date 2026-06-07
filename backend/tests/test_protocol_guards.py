"""
SSE protocol consistency guards.

Tests the import-time drift detection mechanism that prevents the server
from starting if Schema ↔ Python event types are out of sync.

This is the Python-side equivalent of Rust's compile-time borrow checker:
you cannot run the program with an inconsistent protocol definition.
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
SCHEMA_PATH = PROJECT_ROOT / "contracts" / "sse-events.schema.json"


class TestImportTimeGuard:
    """Verify that sse_events.py raises ImportError on schema drift."""

    def test_normal_import_succeeds(self):
        """Baseline: import should succeed when schema and Python are aligned."""
        from src.types.sse_events import EVENT_TAG_MAP, _verify_protocol_consistency
        assert len(EVENT_TAG_MAP) == 10
        # If we got here, _verify_protocol_consistency() already ran at import time

    def test_guard_detects_schema_drift(self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
        """If schema has an extra event type, import should fail."""
        # Read current schema
        schema = json.loads(SCHEMA_PATH.read_text())

        # Create a drifted schema with a fake event type
        drifted_schema = json.loads(json.dumps(schema))  # deep copy
        drifted_schema["$defs"]["fake_event"] = {
            "allOf": [
                {"$ref": "#/$defs/envelope"},
                {
                    "type": "object",
                    "properties": {"event": {"const": "fake_event"}},
                    "required": ["event"],
                },
            ]
        }

        # Write drifted schema to tmp_path
        drifted_schema_file = tmp_path / "sse-events.schema.json"
        drifted_schema_file.write_text(json.dumps(drifted_schema))

        # Point the module to the drifted schema
        monkeypatch.setattr(
            "src.types.sse_events._load_schema_event_types",
            lambda: {"fake_event"} | set(schema["$defs"].keys()),
        )

        # Re-run verification - should raise
        from src.types.sse_events import _verify_protocol_consistency

        with pytest.raises(ImportError, match="SSE protocol drift detected"):
            _verify_protocol_consistency()

    def test_guard_detects_python_drift(self, monkeypatch: pytest.MonkeyPatch):
        """If Python has an extra event type, import should fail."""
        from src.types import sse_events

        # Temporarily add a fake event type to EVENT_TAG_MAP
        original_map = sse_events.EVENT_TAG_MAP.copy()
        sse_events.EVENT_TAG_MAP["fake_python_event"] = None  # type: ignore

        try:
            with pytest.raises(ImportError, match="SSE protocol drift detected"):
                sse_events._verify_protocol_consistency()
        finally:
            sse_events.EVENT_TAG_MAP.clear()
            sse_events.EVENT_TAG_MAP.update(original_map)


class TestCrossLanguageScript:
    """Verify that scripts/check_sse_protocol.py works correctly."""

    def test_script_passes_on_aligned_codebase(self):
        """Baseline: script should exit 0 when all three sources are aligned."""
        result = subprocess.run(
            [sys.executable, str(PROJECT_ROOT / "scripts" / "check_sse_protocol.py")],
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
        )
        assert result.returncode == 0, f"Script failed:\n{result.stdout}\n{result.stderr}"
        assert "All three sources aligned" in result.stdout

    def test_script_lists_all_event_types(self):
        """Script should list all 10 event types in output."""
        result = subprocess.run(
            [sys.executable, str(PROJECT_ROOT / "scripts" / "check_sse_protocol.py")],
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
        )
        expected_events = {
            "thinking", "clarification", "criteria_card", "text_delta",
            "product_card", "cart_action", "final_decision", "done",
            "error", "compare_card",
        }
        for event in expected_events:
            assert event in result.stdout, f"Event type '{event}' not listed in output"


class TestMakeTarget:
    """Verify that 'make protocol-check' is wired up correctly."""

    def test_makefile_has_protocol_check_target(self):
        """Makefile should have a protocol-check target."""
        makefile = PROJECT_ROOT / "Makefile"
        assert makefile.exists()
        content = makefile.read_text()
        assert "protocol-check:" in content
        assert "check_sse_protocol.py" in content
