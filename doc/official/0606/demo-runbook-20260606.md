# Demo Runbook — 2026-06-06

版本：v1.0
日期：2026-06-06
目的：供评委或答辩人按步骤操作 8 条核心 Demo 路径，验证全链路可演示。

---

## Demo 1：文本导购

| 项目 | 内容 |
|------|------|
| 目标 | 验证模糊推荐 + 条件筛选：意图识别 → 购买标准 → 混合检索 → 推荐生成 → 商品卡渲染 |
| 输入话术 | `推荐适合油性肌肤的日常洁面，200 元内` |
| 预期 SSE 事件 | `thinking` → `criteria_card`(chips: 洁面类/油性肌肤/200元以内) → `text_delta` → `product_card`×3+ → `final_decision` → `done` |
| 预期商品类别 | 美妆护肤 > 洁面；预期候选包含氨基酸洁面、泡沫洁面类商品 |
| Android 展示点 | Criteria 卡片显示筛选条件 chips；商品卡展示证据标签；决策卡展示 winner + 理由 |
| 失败兜底话术 | 若检索无结果：`当前资料没有找到完全匹配的商品，建议放宽预算或肤质条件` |

## Demo 2：多轮决策

| 项目 | 内容 |
|------|------|
| 目标 | 验证多轮上下文：第二轮细化约束后 criteria patch + 候选替换 |
| 前置 | 完成 Demo 1 |
| 输入话术 | `有没有更温和一点的` |
| 预期 SSE 事件 | `thinking` → `criteria_card`(patch: 温和/氨基酸优先) → `product_card`×新候选 → `final_decision` → `done` |
| 预期商品变化 | 候选中刺激性更低的产品排名提升 |
| Android 展示点 | 新一轮 Criteria 卡显示"温和"约束；商品卡更新 |
| 失败兜底话术 | `已按更温和方向调整，如果候选变化不大，可以试试明确排除香精成分` |

## Demo 3：主动反问

| 项目 | 内容 |
|------|------|
| 目标 | 验证信息不足时主动澄清：clarification 事件 + suggested_options |
| 输入话术 | `推荐一款手机` |
| 预期 SSE 事件 | `thinking` → `text_delta`(分析说明) → `clarification`(question + suggested_options: 拍照/续航/性价比/游戏) → `done` |
| 预期商品类别 | 数码电子；但此轮不检索，等用户选择 |
| Android 展示点 | Clarification 卡片展示偏好选项按钮；用户点击后注入下一轮 criteria |
| 失败兜底话术 | `如果你还没想好偏好，我可以先按综合性价比推荐` |

## Demo 4：图片导购

| 项目 | 内容 |
|------|------|
| 目标 | 验证拍照找货⭐⭐⭐：VLM 理解 → image embedding → visual recall → 商品卡 |
| 输入话术 | 上传一张护肤品图片 + `这个适合敏感肌吗` |
| 预期 SSE 事件 | `thinking(stage=image_analysis)` → `criteria_card` → `product_card`×含 visual_recall 证据 → `final_decision` → `done` |
| 预期商品类别 | 美妆护肤 > 与上传图片同品类 |
| Android 展示点 | 图片缩略图在用户气泡展示；商品卡显示 visual recall 证据标签 |
| 失败兜底话术 | `图片识别不够稳定，可以换一张更清晰的商品正面图再试` |

## Demo 5：场景组合

| 项目 | 内容 |
|------|------|
| 目标 | 验证 travel 场景化组合推荐：跨品类 criteria + 组合推荐 |
| 输入话术 | `下周去三亚度假，帮我搭配一套从防晒到穿搭的方案` |
| 预期 SSE 事件 | `thinking` → `criteria_card`(shopping_strategy.scene_type=travel, 跨品类) → `product_card`×多品类 → `final_decision` → `done` |
| 预期商品类别 | 美妆护肤(防晒) + 服饰运动(泳装/配饰) |
| Android 展示点 | Criteria 卡展示"三亚度假搭配方案"叙事；商品卡涵盖多个品类 |
| 失败兜底话术 | `跨品类检索还在完善，当前先给出防晒方向的推荐` |

## Demo 6：购物车管理

| 项目 | 内容 |
|------|------|
| 目标 | 验证对话式购物车 CRUD：add → update_quantity → remove |
| 前置 | 完成 Demo 1，已有推荐结果 |
| 输入话术 | 1. `把第一个加入购物车` → 2. `数量改成 2` → 3. `删除第二个` |
| 预期 SSE 事件 | 三次 `cart_action`(add/update_quantity/remove) + 每轮 `done` |
| 预期商品变化 | 购物车先增后改数量再删除 |
| Android 展示点 | 每次操作显示 CartActionCard 回执；打开购物车 sheet 查看明细 |
| 失败兜底话术 | `购物车操作没有成功，可以试试直接说"查看购物车"` |

## Demo 7：购买意向确认

| 项目 | 内容 |
|------|------|
| 目标 | 验证下单确认闭环⭐⭐⭐：checkout_preview → confirm → audit |
| 前置 | 购物车非空（Demo 6 已加购） |
| 输入话术 | 1. `就买这个` → 2. 点击预览卡上的"确认购买" |
| 预期 SSE 事件 | 1. `cart_action(checkout_preview, status=success)` → 2. `cart_action(checkout_confirm, status=success)` |
| 预期展示 | 1. 确认购买意向卡（商品列表 + 总价 + "不涉及真实支付"提示）→ 2. "购买意向已确认"回执 |
| Android 展示点 | CheckoutPreviewCard 展示商品明细 + 确认/取消按钮；确认后显示确认回执 |
| 失败兜底话术 | 购物车为空时：`购物车为空，先把商品加入购物车` |

## Demo 8：对比决策

| 项目 | 内容 |
|------|------|
| 目标 | 验证多商品对比决策⭐⭐⭐：compare_card + narration |
| 前置 | 完成 Demo 1 或 Demo 2，有多个推荐商品 |
| 输入话术 | `对比第一个和第二个` 或点击决策卡"加入对比"按钮 |
| 预期 SSE 事件 | `thinking` → `compare_card`(axes + winner) → `text_delta`(narration) → `done` |
| 预期展示 | CompareCard 展示对比轴 + 胜出者 + 取舍说明 |
| Android 展示点 | CompareSummaryCard 展示对比表格 + 结论 |
| 失败兜底话术 | `对比需要至少两个候选，先推荐一些商品再对比` |

---

## 执行顺序建议

```
Demo 1 → Demo 2 → Demo 6 → Demo 7 → Demo 3 → Demo 4 → Demo 5 → Demo 8
```

先走通基础链路 + 购物车 + 意向确认（P0 核心），再做主动反问和多模态，最后对比。

## 注意事项

1. 每条 Demo 开始前确认后端已启动且 SSE 连接正常。
2. Demo 7 的文案必须统一为"确认购买意向"，不能出现"下单""支付""订单"等误导性表述。
3. 图片 Demo 需要准备一张清晰的护肤品商品图。
4. 如果 LLM 响应不稳定，可以重试，但记录重试次数。
