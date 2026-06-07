#!/usr/bin/env python3
"""
Cross-language SSE protocol consistency check.

Verifies that the three sources of truth are aligned:
  1. contracts/sse-events.schema.json (Schema)
  2. backend/src/types/sse_events.py (Python)
  3. android/core/model/src/main/java/com/buypilot/core/model/AgentEventType.kt (Kotlin)

Run via: make protocol-check
Or manually: python scripts/check_sse_protocol.py

Exit code 0 = all three sources aligned
Exit code 1 = drift detected (with detailed error message)
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SCHEMA_PATH = PROJECT_ROOT / "contracts" / "sse-events.schema.json"
PYTHON_EVENTS_PATH = PROJECT_ROOT / "backend" / "src" / "types" / "sse_events.py"
KOTLIN_EVENTS_PATH = (
    PROJECT_ROOT
    / "android"
    / "core"
    / "model"
    / "src"
    / "main"
    / "java"
    / "com"
    / "buypilot"
    / "core"
    / "model"
    / "AgentEventType.kt"
)


def extract_schema_event_types() -> set[str]:
    """Extract event type names from JSON Schema $defs."""
    if not SCHEMA_PATH.exists():
        raise FileNotFoundError(f"Schema not found: {SCHEMA_PATH}")

    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    types: set[str] = set()

    for name, defn in schema.get("$defs", {}).items():
        for part in defn.get("allOf", []):
            event_const = part.get("properties", {}).get("event", {}).get("const")
            if event_const:
                types.add(event_const)

    return types


def extract_python_event_types() -> set[str]:
    """Extract event type names from Python EVENT_TAG_MAP."""
    if not PYTHON_EVENTS_PATH.exists():
        raise FileNotFoundError(f"Python events not found: {PYTHON_EVENTS_PATH}")

    content = PYTHON_EVENTS_PATH.read_text(encoding="utf-8")

    # Parse EVENT_TAG_MAP keys: EVENT_TAG_MAP = { "thinking": ..., "clarification": ..., ... }
    # Use regex to extract keys from the dict literal
    match = re.search(
        r"EVENT_TAG_MAP\s*:\s*dict\[str,\s*type\[SSEEvent\]\]\s*=\s*\{([^}]+)\}",
        content,
        re.DOTALL,
    )
    if not match:
        raise ValueError("Could not parse EVENT_TAG_MAP from sse_events.py")

    map_body = match.group(1)
    # Extract quoted keys: "thinking", "clarification", etc.
    keys = re.findall(r'"([^"]+)":\s*\w+Event', map_body)
    return set(keys)


def extract_kotlin_event_types() -> set[str]:
    """Extract event type wire values from Kotlin enum.

    Excludes 'unknown' which is an intentional fallback for unrecognized events.
    """
    if not KOTLIN_EVENTS_PATH.exists():
        raise FileNotFoundError(f"Kotlin events not found: {KOTLIN_EVENTS_PATH}")

    content = KOTLIN_EVENTS_PATH.read_text(encoding="utf-8")

    # Parse enum entries: ThinkingEvent("thinking"), ClarificationEvent("clarification"), ...
    # Pattern: identifier("wire_value")
    matches = re.findall(r'\w+\("([^"]+)"\)', content)
    # 'unknown' is a deliberate fallback, not a protocol event type
    return set(matches) - {"unknown"}


def main() -> int:
    print("Checking SSE protocol consistency across 3 sources of truth...")
    print()

    try:
        schema_types = extract_schema_event_types()
        print(f"✓ Schema: {len(schema_types)} event types")
    except Exception as e:
        print(f"✗ Failed to parse schema: {e}")
        return 1

    try:
        python_types = extract_python_event_types()
        print(f"✓ Python: {len(python_types)} event types")
    except Exception as e:
        print(f"✗ Failed to parse Python: {e}")
        return 1

    try:
        kotlin_types = extract_kotlin_event_types()
        print(f"✓ Kotlin: {len(kotlin_types)} event types")
    except Exception as e:
        print(f"✗ Failed to parse Kotlin: {e}")
        return 1

    print()

    # Check all pairwise combinations
    errors: list[str] = []

    # Schema vs Python
    schema_not_python = schema_types - python_types
    python_not_schema = python_types - schema_types

    if schema_not_python:
        errors.append(
            f"Schema defines event types not in Python: {sorted(schema_not_python)}\n"
            f"  → Add Pydantic classes to backend/src/types/sse_events.py"
        )

    if python_not_schema:
        errors.append(
            f"Python defines event types not in Schema: {sorted(python_not_schema)}\n"
            f"  → Update contracts/sse-events.schema.json (Schema is source of truth)"
        )

    # Schema vs Kotlin
    schema_not_kotlin = schema_types - kotlin_types
    kotlin_not_schema = kotlin_types - schema_types

    if schema_not_kotlin:
        errors.append(
            f"Schema defines event types not in Kotlin: {sorted(schema_not_kotlin)}\n"
            f"  → Add enum entries to AgentEventType.kt"
        )

    if kotlin_not_schema:
        errors.append(
            f"Kotlin defines event types not in Schema: {sorted(kotlin_not_schema)}\n"
            f"  → Update contracts/sse-events.schema.json (Schema is source of truth)"
        )

    if errors:
        print("✗ SSE protocol drift detected:\n")
        for i, error in enumerate(errors, 1):
            print(f"{i}. {error}\n")
        print("Fix the drift, then re-run: make protocol-check")
        return 1
    else:
        print("✓ All three sources aligned: Schema ↔ Python ↔ Kotlin")
        print()
        print("Event types:")
        for event_type in sorted(schema_types):
            print(f"  • {event_type}")
        return 0


if __name__ == "__main__":
    sys.exit(main())
