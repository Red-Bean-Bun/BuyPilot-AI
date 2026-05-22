"""BuyPilot evaluation CLI — run eval from the command line.

Usage:
    cd backend
    python -m src.scripts.eval --strategy baseline
    python -m src.scripts.eval --strategy "baseline+rerank" --prompt-version v1.1.0
"""

from __future__ import annotations

import argparse
import asyncio
import json
import subprocess
import sys
from pathlib import Path

BACKEND_DIR = Path(__file__).resolve().parents[1]
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))


def _get_git_commit() -> str | None:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            capture_output=True,
            text=True,
            timeout=5,
        )
        return result.stdout.strip() if result.returncode == 0 else None
    except Exception:
        return None


async def _main() -> None:
    parser = argparse.ArgumentParser(description="BuyPilot Eval Runner")
    parser.add_argument(
        "--strategy",
        default="baseline",
        help="Strategy tag for this eval run (default: baseline)",
    )
    parser.add_argument(
        "--run-name",
        default=None,
        help="Custom run name (auto-generated if omitted)",
    )
    parser.add_argument(
        "--prompt-version",
        default=None,
        help="Prompt version tag (e.g. v1.0.0)",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output raw JSON to stdout instead of formatted text",
    )
    args = parser.parse_args()

    from src.services.eval.runner import run_eval

    git_commit = _get_git_commit()
    print(f"Starting eval run: strategy={args.strategy}, git={git_commit}")

    result = await run_eval(
        strategy_tag=args.strategy,
        run_name=args.run_name,
        prompt_version=args.prompt_version,
        git_commit=git_commit,
    )

    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print("\n=== Eval Results ===")
        print(f"Overall Score: {result.get('overall_score', 'N/A')}")
        print()
        for key, val in result.items():
            if key == "overall_score":
                continue
            if isinstance(val, dict):
                mean = val.get("mean", "N/A")
                print(f"  {key}: {mean}")


def main() -> None:
    asyncio.run(_main())


if __name__ == "__main__":
    main()
