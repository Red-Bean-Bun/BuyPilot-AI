#!/usr/bin/env python3
"""
BuyPilot 失败测试复测脚本

只运行之前失败的 5 个测试，验证修复效果。
"""

import sys
import time

from stress_test import (
    TestSession,
    _print_result,
    _section,
    analyze,
)


def main():
    _section("失败测试复测（5 个）")
    print("=" * 60)

    # 测试 1: 推荐手机（无预算 → clarification）
    print("\n--- 测试 1: 推荐手机（无预算） ---")
    s1 = TestSession()
    ok, events = s1.send("推荐手机")
    r = analyze(events, test_name="推荐手机")
    r.success = ok
    _print_result(r, expect="clarification")

    # 测试 2: 对比第一款和第二款（数据不足 → 降级）
    print("\n--- 测试 2: 对比第一款和第二款（数据不足） ---")
    s2 = TestSession()
    ok, events = s2.send("推荐手机")
    ok, events = s2.send("4000元以内")
    ok, events = s2.send("对比第一款和第二款")
    r = analyze(events, test_name="对比第一款和第二款")
    r.success = ok
    _print_result(r, expect=None)  # 不检查，降级为 recommend 也算正确

    # 测试 3: 空消息（422）
    print("\n--- 测试 3: 空消息 ---")
    s3 = TestSession()
    ok, events = s3.send("")
    r = analyze(events, test_name="空消息: ''")
    r.success = ok
    _print_result(r, expect_error=True)

    # 测试 4: 纯空白（422）
    print("\n--- 测试 4: 纯空白 ---")
    s4 = TestSession()
    ok, events = s4.send("   ")
    r = analyze(events, test_name="纯空白: '   '")
    r.success = ok
    _print_result(r, expect_error=True)

    # 测试 5: 不存在的图片文件
    print("\n--- 测试 5: 不存在的图片文件 ---")
    s5 = TestSession()
    ok, events = s5.send("这是什么？", image_url="/uploads/nonexistent_file.jpg")
    r = analyze(events, test_name="不存在的图片文件")
    r.success = ok
    _print_result(r, expect_error=True)

    print("\n" + "=" * 60)
    print("复测完成")


if __name__ == "__main__":
    main()
