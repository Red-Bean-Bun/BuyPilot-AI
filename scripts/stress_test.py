#!/usr/bin/env python3
"""
BuyPilot 综合爆破测试脚本

用法:
    python scripts/stress_test.py                  # 运行全部测试
    python scripts/stress_test.py --phase basic    # 只运行基础功能测试
    python scripts/stress_test.py --phase compare  # 只运行对比功能测试
    python scripts/stress_test.py --list           # 列出所有测试阶段

测试阶段:
    basic    - P1: 单轮推荐、多轮对话、基础购物车
    compare  - P2: 对比功能专项验证
    cart     - P3: 购物车深度测试（边界、管理）
    checkout - P4: 购买流程（确认/取消）
    edge     - P5: 边界输入（空消息、超长、特殊字符）
    intent   - P6: 意图边界（闲聊、歧义、混淆）
    stress   - P7: 并发压力和状态一致性

如何添加新测试:
    1. 在对应的 phase 函数中添加新的 multi_turn() 或 single_turn() 调用
    2. 或者创建新的 phase 函数，在 PHASES 字典中注册
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import sys
import time
import uuid
from dataclasses import dataclass, field
from typing import Any

import requests

# ─── 配置 ────────────────────────────────────────────────────────────────
BASE_URL = "http://localhost:8000"
ADMIN_KEY = "b72d57075018654b8d7ad1ab6e71fb0be3f2ec02fc300015"
DEFAULT_DELAY = 0.8  # 请求间隔（秒）


# ─── 基础设施 ─────────────────────────────────────────────────────────────
@dataclass
class AnalysisResult:
    """事件流分析结果"""
    test_name: str = ""
    success: bool = True
    error_message: str | None = None
    event_types: list[str] = field(default_factory=list)
    intent: str | None = None
    has_product_card: bool = False
    has_compare_card: bool = False
    has_cart_action: bool = False
    has_clarification: bool = False
    has_error: bool = False
    product_count: int = 0
    cart_action_type: str | None = None
    cart_product_id: str | None = None
    compare_ids: list[str] = field(default_factory=list)


class TestSession:
    """管理一个测试会话，维持 session_id 跨多轮对话"""

    def __init__(self, session_id: str | None = None):
        self.session_id = session_id or f"stress_{uuid.uuid4().hex[:12]}"
        self.turn_counter = 0

    def send(self, message: str, image_url: str | None = None) -> tuple[bool, list[dict]]:
        """发送消息，返回 (success, events)"""
        self.turn_counter += 1
        turn_id = f"turn_{self.turn_counter:03d}"

        body: dict[str, Any] = {
            "session_id": self.session_id,
            "client_turn_id": turn_id,
            "message": message,
        }
        if image_url:
            body["image_url"] = image_url

        headers = {
            "Authorization": f"Bearer {ADMIN_KEY}",
            "Content-Type": "application/json",
        }

        try:
            response = requests.post(
                f"{BASE_URL}/chat/stream",
                json=body,
                headers=headers,
                stream=True,
                timeout=30,
            )
            response.raise_for_status()

            events = []
            for line in response.iter_lines():
                if not line:
                    continue
                line_str = line.decode("utf-8")
                if line_str.startswith("data: "):
                    event_data = line_str[6:].strip()
                    if event_data:
                        try:
                            events.append(json.loads(event_data))
                        except json.JSONDecodeError:
                            pass
            return True, events
        except Exception as e:
            return False, [{"event": "error", "payload": {"message": str(e)}}]


def analyze(events: list[dict], test_name: str = "") -> AnalysisResult:
    """分析事件流，提取关键信息"""
    r = AnalysisResult(test_name=test_name)

    for event in events:
        etype = event.get("event", "unknown")
        r.event_types.append(etype)

        if etype == "product_card":
            r.has_product_card = True
            r.product_count += 1
        elif etype == "compare_card":
            r.has_compare_card = True
            r.compare_ids = event.get("product_ids", [])
        elif etype == "cart_action":
            r.has_cart_action = True
            r.cart_action_type = event.get("action")
            r.cart_product_id = event.get("product_id")
        elif etype == "clarification":
            r.has_clarification = True
        elif etype == "error":
            r.has_error = True
            r.error_message = event.get("payload", {}).get("message")

    return r


# ─── 输出 ─────────────────────────────────────────────────────────────────
_stats = {"pass": 0, "warn": 0, "fail": 0, "total": 0}


def _print_result(r: AnalysisResult, expect: str | None = None):
    """打印单个测试结果

    expect: 期望的事件类型，可选值:
        "product_card" - 期望有商品卡片
        "compare_card" - 期望有对比卡片
        "cart_action"  - 期望有购物车操作
        "clarification" - 期望有澄清提问
        "no_error"     - 期望没有错误
        None           - 不检查期望
    """
    _stats["total"] += 1

    if r.has_error:
        status = "❌"
        _stats["fail"] += 1
    elif expect == "product_card" and not r.has_product_card:
        status = "❌"
        _stats["fail"] += 1
    elif expect == "compare_card" and not r.has_compare_card:
        status = "❌"
        _stats["fail"] += 1
    elif expect == "cart_action" and not r.has_cart_action:
        status = "❌"
        _stats["fail"] += 1
    elif expect == "clarification" and not r.has_clarification:
        status = "❌"
        _stats["fail"] += 1
    elif expect == "no_error" and r.has_error:
        status = "❌"
        _stats["fail"] += 1
    elif not r.has_product_card and not r.has_compare_card and not r.has_cart_action:
        status = "⚠️"
        _stats["warn"] += 1
    else:
        status = "✅"
        _stats["pass"] += 1

    print(f"\n{status} {r.test_name}")
    events_str = " → ".join(r.event_types[:20])
    if len(r.event_types) > 20:
        events_str += f" ... (+{len(r.event_types) - 20})"
    print(f"  事件: {events_str}")

    if r.has_error:
        print(f"  🔴 错误: {r.error_message}")
    if r.has_product_card:
        print(f"  📦 商品卡片: {r.product_count} 个")
    if r.has_compare_card:
        print(f"  📊 对比卡片: {r.compare_ids or '(触发)'}")
    if r.has_cart_action:
        print(f"  🛒 购物车: {r.cart_action_type} → {r.cart_product_id}")
    if r.has_clarification and not r.has_product_card:
        print(f"  ❓ 澄清提问")


def _section(title: str):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


# ─── 便捷方法 ─────────────────────────────────────────────────────────────
def single_turn(
    session: TestSession,
    message: str,
    expect: str | None = None,
    delay: float = DEFAULT_DELAY,
):
    """发送单条消息并打印结果"""
    ok, events = session.send(message)
    r = analyze(events, test_name=message)
    r.success = ok
    _print_result(r, expect=expect)
    if delay > 0:
        time.sleep(delay)


def multi_turn(
    messages: list[str],
    expects: list[str | None] | None = None,
    session: TestSession | None = None,
    delay: float = DEFAULT_DELAY,
):
    """多轮对话测试，共享同一个 session"""
    if expects is None:
        expects = [None] * len(messages)
    if session is None:
        session = TestSession()
    for msg, exp in zip(messages, expects):
        single_turn(session, msg, expect=exp, delay=delay)


# ─── Phase 1: 基础功能 ────────────────────────────────────────────────────
def phase_basic():
    """单轮推荐、多轮对话、基础购物车"""
    _section("Phase 1: 单轮推荐（基础功能）")

    multi_turn(
        ["推荐一款洗面奶",
         "推荐适合油皮的洗面奶，200元以内",
         "有没有耳机",
         "推荐一款电动车"],
        expects=["product_card"] * 4,
    )

    _section("Phase 2: 多轮对话（进阶场景）")

    print("\n--- 2.1 约束叠加 ---")
    multi_turn(
        ["推荐跑鞋", "要轻量的", "预算500以内"],
        expects=["product_card"] * 3,
    )

    print("\n--- 2.2 反选排除 ---")
    multi_turn(
        ["推荐防晒霜", "不要含酒精的"],
        expects=["product_card"] * 2,
    )

    print("\n--- 2.3 换品类 ---")
    multi_turn(
        ["推荐手机", "算了，看看耳机吧"],
    )

    print("\n--- 2.4 追问 ---")
    multi_turn(
        ["推荐一款洗面奶", "再便宜点的呢？"],
        expects=["product_card"] * 2,
    )

    _section("Phase 3: 基础购物车")

    print("\n--- 3.1 明确加购 ---")
    multi_turn(
        ["推荐一款洗面奶", "把第一个加到购物车"],
        expects=["product_card", "cart_action"],
    )

    print("\n--- 3.2 模糊加购 ---")
    multi_turn(
        ["推荐一款洗面奶", "这个不错，帮我加上"],
        expects=["product_card", "cart_action"],
    )

    print("\n--- 3.3 空购物车加购 ---")
    single_turn(TestSession(), "加到购物车")

    print("\n--- 3.4 购物车管理 ---")
    multi_turn(
        ["推荐一款洗面奶", "把第一个加到购物车",
         "删掉第二个", "数量改成2", "看看购物车"],
        expects=["product_card", "cart_action", "cart_action",
                 "cart_action", "cart_action"],
    )


# ─── Phase 4: 对比功能 ────────────────────────────────────────────────────
def phase_compare():
    """对比功能专项验证"""
    _section("Phase 4: 对比功能专项测试")

    print("\n--- 4.1 基础对比（手机/笔记本有多商品） ---")
    multi_turn(
        ["推荐手机", "对比第一个和第二个", "对比前两款"],
        expects=["product_card", "compare_card", "compare_card"],
    )

    print("\n--- 4.2 复杂对比表达 ---")
    multi_turn(
        ["推荐笔记本电脑", "第一款和第二款哪个好",
         "第一款和第三款哪个好"],
        expects=["product_card", "compare_card", "compare_card"],
    )

    print("\n--- 4.3 对比失败场景（数据不足） ---")
    multi_turn(
        ["推荐一款洗面奶", "对比一下"],
        # 洗面奶只有1个商品，对比应该重分类为 recommend
    )

    print("\n--- 4.4 多轮对比后决策 ---")
    multi_turn(
        ["推荐手机", "4000元以内", "对比第一款和第二款"],
        expects=[None, "product_card", "compare_card"],
    )


# ─── Phase 5: 购物车深度 ──────────────────────────────────────────────────
def phase_cart():
    """购物车边界和管理"""
    _section("Phase 5: 购物车深度测试")

    print("\n--- 5.1 数量边界 ---")
    multi_turn(
        ["推荐洗面奶", "加100个到购物车", "加0个"],
        expects=["product_card", "no_error", "no_error"],
    )

    print("\n--- 5.2 模糊/越界加购 ---")
    multi_turn(
        ["推荐洗面奶",
         "把第100个加入购物车",   # 不存在的序号
         "把最后一个加入购物车",  # "最后一个" 表达
         "加5个到购物车",        # 数量不明确
         "删除购物车",           # 模糊指令
         "清空购物车"],          # 清空指令
        expects=["product_card"] + ["no_error"] * 5,
    )


# ─── Phase 6: 购买流程 ────────────────────────────────────────────────────
def phase_checkout():
    """购买确认/取消流程"""
    _section("Phase 6: 购买流程")

    print("\n--- 6.1 确认购买 ---")
    multi_turn(
        ["推荐一款洗面奶", "就买这个"],
        expects=["product_card", "cart_action"],
    )

    print("\n--- 6.2 取消购买 ---")
    multi_turn(
        ["推荐一款洗面奶", "就买这个", "算了不买"],
        expects=["product_card", "cart_action", "cart_action"],
    )

    print("\n--- 6.3 短词购买（上下文感知） ---")
    multi_turn(
        ["推荐洗面奶", "把第一个加入购物车", "购买"],
        expects=["product_card", "cart_action", "cart_action"],
    )


# ─── Phase 7: 边界输入 ────────────────────────────────────────────────────
def phase_edge():
    """异常输入和边界条件"""
    _section("Phase 7: 边界输入")

    edge_cases = [
        ("", "空消息"),
        ("   ", "纯空白"),
        ("a" * 2000, "超长文本 (2000字)"),
        ("推荐" + "？" * 100, "重复标点"),
        ("推荐🔥💰手机", "emoji 混合"),
        ("推 荐 洗 面 奶", "空格分隔"),
        ("RECOMMEND FACE WASH", "纯英文"),
        ("推荐\n洗面奶\n200元", "换行分隔"),
        ("推荐<脚本>alert(1)</脚本>", "XSS 尝试"),
        ("推荐 face wash 洗面奶", "中英混合"),
        ("推荐" + "洗面奶" * 500, f"超长文本 ({1502}字)"),
    ]

    s = TestSession()
    for msg, label in edge_cases:
        ok, events = s.send(msg)
        r = analyze(events, test_name=f"{label}: {repr(msg)[:40]}")
        r.success = ok
        # 空/空白消息预期返回 error (422)，其他预期 no_error
        if msg.strip() == "":
            _print_result(r, expect=None)  # 不检查，422 也算正常
        else:
            _print_result(r, expect="no_error")
        time.sleep(0.5)


# ─── Phase 8: 意图边界 ────────────────────────────────────────────────────
def phase_intent():
    """意图识别边界和混淆"""
    _section("Phase 8: 意图识别边界")

    print("\n--- 8.1 闲聊/问候 ---")
    multi_turn(
        ["今天天气怎么样", "你好", "你是谁", "帮我看看这个"],
    )

    print("\n--- 8.2 指代不明 ---")
    multi_turn(
        ["这个怎么样", "哪个好", "便宜一点", "还是算了"],
        expects=["no_error"] * 4,
    )

    print("\n--- 8.3 反馈/替换（有上下文） ---")
    multi_turn(
        ["推荐洗面奶", "换一个", "再换一个", "不要这个", "不要第一款"],
        expects=["product_card"] + ["no_error"] * 4,
    )


# ─── Phase 9: 并发压力 ────────────────────────────────────────────────────
def phase_stress():
    """并发压力和快速连续请求"""
    _section("Phase 9: 并发压力测试")

    print("\n--- 9.1 5 并发推荐请求 ---")

    def concurrent_request(idx: int) -> AnalysisResult:
        s = TestSession(f"concurrent_{idx}")
        ok, events = s.send("推荐洗面奶")
        r = analyze(events, test_name=f"并发 #{idx}")
        r.success = ok
        return r

    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        futures = [pool.submit(concurrent_request, i) for i in range(5)]
        for f in concurrent.futures.as_completed(futures):
            r = f.result()
            _print_result(r, expect="product_card")

    print("\n--- 9.2 快速连续请求（同一会话） ---")
    s = TestSession()
    for i in range(10):
        msg = f"推荐第{i+1}款"
        ok, events = s.send(msg)
        r = analyze(events, test_name=msg)
        r.success = ok
        _print_result(r)
        time.sleep(0.3)


# ─── Phase 10: 多品类 Demo 路径 ───────────────────────────────────────────
def phase_demo():
    """4 条 Demo 路径完整验证"""
    _section("Phase 10: Demo 路径验证")

    print("\n--- Demo 1: 模糊推荐+条件筛选（美妆护肤） ---")
    multi_turn(
        ["推荐适合油皮的洗面奶，200元以内"],
        expects=["product_card"],
    )

    print("\n--- Demo 3: 反选排除+多轮约束 ---")
    multi_turn(
        ["推荐防晒霜", "不要含酒精的", "预算降到200"],
        expects=["product_card"] * 3,
    )

    print("\n--- Demo 4: 对话式加购 ---")
    multi_turn(
        ["推荐洗面奶", "把第一个加到购物车", "看看购物车"],
        expects=["product_card", "cart_action", "cart_action"],
    )

    print("\n--- 旅行场景（跨品类组合） ---")
    multi_turn(
        ["下周去三亚度假，帮我搭配一套防晒到穿搭的方案"],
    )


# ─── 注册表 ───────────────────────────────────────────────────────────────
PHASES = {
    "basic":   ("基础功能", phase_basic),
    "compare": ("对比功能", phase_compare),
    "cart":    ("购物车深度", phase_cart),
    "checkout": ("购买流程", phase_checkout),
    "edge":    ("边界输入", phase_edge),
    "intent":  ("意图边界", phase_intent),
    "stress":  ("并发压力", phase_stress),
    "demo":    ("Demo 路径", phase_demo),
}


def run_all():
    """运行全部测试"""
    for key, (_, fn) in PHASES.items():
        fn()


def print_summary():
    """打印测试总结"""
    total = _stats["total"]
    passed = _stats["pass"]
    warned = _stats["warn"]
    failed = _stats["fail"]
    rate = (passed / total * 100) if total > 0 else 0

    _section("测试总结")
    print(f"  总计: {total}  通过: {passed}  警告: {warned}  失败: {failed}")
    print(f"  通过率: {rate:.0f}%")
    if failed > 0:
        print(f"  🔴 有 {failed} 个测试失败，请检查上面的输出")
    print()


def main():
    global DEFAULT_DELAY

    parser = argparse.ArgumentParser(
        description="BuyPilot 综合爆破测试",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "--phase", "-p",
        choices=list(PHASES.keys()),
        help="只运行指定测试阶段",
    )
    parser.add_argument(
        "--list", "-l",
        action="store_true",
        help="列出所有测试阶段",
    )
    parser.add_argument(
        "--delay", "-d",
        type=float,
        default=DEFAULT_DELAY,
        help=f"请求间隔秒数 (默认 {DEFAULT_DELAY})",
    )
    args = parser.parse_args()

    DEFAULT_DELAY = args.delay

    if args.list:
        print("可用测试阶段:")
        for key, (name, _) in PHASES.items():
            print(f"  {key:10s} - {name}")
        return

    print("=" * 60)
    print("  BuyPilot 综合爆破测试")
    print("=" * 60)

    if args.phase:
        name, fn = PHASES[args.phase]
        print(f"  运行阶段: {name}")
        fn()
    else:
        run_all()

    print_summary()


if __name__ == "__main__":
    main()
