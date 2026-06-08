#!/usr/bin/env python3
"""
对比功能专项验证 - 使用数码电子品类（智能手机 10 个）

Development tool - not used in production or evaluation.
Usage: local verification of multi-product compare feature.
"""

import os
from pathlib import Path

import requests
import json
import time
import uuid

from dotenv import load_dotenv

BASE_URL = "http://localhost:8000"
load_dotenv(Path(__file__).resolve().parent.parent / ".env")
ADMIN_KEY = os.environ.get("ADMIN_API_KEY", "")


def send_message(session_id: str, message: str) -> dict:
    """发送消息并返回事件列表"""
    payload = {
        "message": message,
        "session_id": session_id
    }
    headers = {"Authorization": f"Bearer {ADMIN_KEY}"}

    try:
        response = requests.post(
            f"{BASE_URL}/chat/stream",
            json=payload,
            headers=headers,
            stream=True
        )
        response.raise_for_status()

        events = []
        for line in response.iter_lines():
            if line:
                line_str = line.decode('utf-8')
                if line_str.startswith('data: '):
                    event_data = line_str[6:]
                    try:
                        event = json.loads(event_data)
                        events.append(event)
                    except json.JSONDecodeError:
                        pass

        return {"success": True, "events": events}
    except Exception as e:
        return {"success": False, "error": str(e), "events": []}


def analyze_events(events: list) -> dict:
    """分析事件流"""
    result = {
        "has_compare_card": False,
        "has_product_card": False,
        "product_count": 0,
        "compare_ids": [],
        "event_types": []
    }

    for event in events:
        event_type = event.get("event")
        result["event_types"].append(event_type)

        if event_type == "compare_card":
            result["has_compare_card"] = True
            result["compare_ids"] = event.get("product_ids", [])

        elif event_type == "product_card":
            result["has_product_card"] = True
            result["product_count"] += 1

    return result


def print_result(test_name: str, result: dict, expected: str):
    """打印测试结果"""
    status = "✅" if result["success"] else "❌"
    analysis = analyze_events(result.get("events", []))

    match = ""
    if expected == "compare_card":
        match = "✅" if analysis["has_compare_card"] else "❌"
    elif expected == "product_card":
        match = "✅" if analysis["has_product_card"] else "❌"
    elif expected == "multiple_products":
        match = "✅" if analysis["product_count"] >= 2 else "❌"

    print(f"\n{status} {test_name} {match}")
    print(f"   事件流: {' → '.join(analysis['event_types'][:15])}")

    if analysis["has_compare_card"]:
        print(f"   📊 对比卡片触发! 商品: {analysis['compare_ids']}")
    if analysis["product_count"] > 0:
        print(f"   📦 商品数量: {analysis['product_count']}")


def main():
    print("="*60)
    print("对比功能验证 - 数码电子品类")
    print("="*60)

    # 测试 1: 推荐手机（应该有多个商品）
    print("\n--- 测试 1: 推荐手机 ---")
    session = str(uuid.uuid4())

    result1 = send_message(session, "推荐手机")
    print_result("推荐手机", result1, "multiple_products")
    time.sleep(1)

    # 测试 2: 对比第一个和第二个
    print("\n--- 测试 2: 对比第一个和第二个 ---")
    result2 = send_message(session, "对比第一个和第二个")
    print_result("对比第一个和第二个", result2, "compare_card")
    time.sleep(1)

    # 测试 3: 推荐平板电脑（验证多品类）
    print("\n--- 测试 3: 推荐平板电脑 ---")
    session2 = str(uuid.uuid4())

    result3 = send_message(session2, "推荐平板电脑")
    print_result("推荐平板电脑", result3, "multiple_products")
    time.sleep(1)

    # 测试 4: 对比前两款
    print("\n--- 测试 4: 对比前两款 ---")
    result4 = send_message(session2, "对比前两款")
    print_result("对比前两款", result4, "compare_card")
    time.sleep(1)

    # 测试 5: 复杂对比表达
    print("\n--- 测试 5: 第一款和第二款哪个好 ---")
    session3 = str(uuid.uuid4())

    result5 = send_message(session3, "推荐笔记本电脑")
    print_result("推荐笔记本电脑", result5, "multiple_products")
    time.sleep(1)

    result6 = send_message(session3, "第一款和第二款哪个好")
    print_result("第一款和第二款哪个好", result6, "compare_card")

    print("\n" + "="*60)
    print("测试完成")
    print("="*60)


if __name__ == "__main__":
    main()
