#!/usr/bin/env python3
"""CLI demo: send a message to /chat/stream and print SSE events.

Development tool - not used in production or evaluation.
Usage: local debugging of SSE event stream.
"""

import json
import sys

import httpx

BASE_URL = "http://localhost:8000"

TAG_ICONS = {
    "thinking": "[THINK]",
    "clarification": "[CLARIFY]",
    "criteria_card": "[CRITERIA]",
    "text_delta": "[STREAM]",
    "product_card": "[PRODUCT]",
    "final_decision": "[DECISION]",
    "done": "[DONE]",
    "error": "[ERROR]",
}


def parse_sse_events(response_text: str):
    import re

    blocks = re.split(r"\n\n+", response_text.strip())
    for block in blocks:
        lines = block.strip().split("\n")
        event_type = None
        data = None
        for line in lines:
            if line.startswith("event: "):
                event_type = line[len("event: "):].strip()
            elif line.startswith("data: "):
                data = json.loads(line[len("data: "):])
        if event_type and data:
            yield event_type, data


def main():
    if len(sys.argv) < 2:
        print("Usage: python chat_stream_demo.py <message>")
        print("Example: python chat_stream_demo.py '给4岁孩子买室内益智玩具，预算200'")
        sys.exit(1)

    message = sys.argv[1]
    print(f"Sending: {message}\n{'='*50}")

    with httpx.stream("POST", f"{BASE_URL}/chat/stream", json={"message": message}) as resp:
        if resp.status_code != 200:
            print(f"Error: HTTP {resp.status_code}")
            sys.exit(1)

        content = b""
        for chunk in resp.iter_bytes():
            content += chunk
            text = content.decode("utf-8")

        for tag, data in parse_sse_events(text):
            label = TAG_ICONS.get(tag, f"[{tag}]")
            if tag == "product_card":
                prod = data.get("product", {})
                print(f"{label} #{data.get('rank')} {prod.get('name', '?')} - {data.get('reason', '')[:60]}")
            elif tag == "thinking":
                print(f"{label} [{data.get('stage', '?')}] {data.get('message', '')}")
            elif tag == "done":
                print(f"{label} session={data.get('session_id', '?')[:8]}...")
            elif tag == "error":
                print(f"{label} {data.get('code')} - {data.get('message')}")
            elif tag == "final_decision":
                print(f"{label} {data.get('summary', '')[:60]}")
            elif tag == "criteria_card":
                criteria = data.get("criteria", {})
                print(f"{label} age={criteria.get('age','?')} budget_max={criteria.get('budget_max','?')}")
            else:
                print(f"{label} {json.dumps(data, ensure_ascii=False)[:80]}")

    print(f"\n{'='*50}")
    print("Done.")


if __name__ == "__main__":
    main()
