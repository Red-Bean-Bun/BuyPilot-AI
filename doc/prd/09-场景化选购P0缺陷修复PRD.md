# 场景化选购 P0 缺陷修复 PRD

**版本**：v1.0  
**日期**：2026-06-07  
**状态**：待实现  
**关联 GitHub 问题单**：[#2](https://github.com/Red-Bean-Bun/BuyPilot-AI/issues/2)、[#3](https://github.com/Red-Bean-Bun/BuyPilot-AI/issues/3)、[#4](https://github.com/Red-Bean-Bun/BuyPilot-AI/issues/4)  
**关联文档**：`doc/prd/07-场景化选购助手PRD.md`

---

## 1. 问题描述

当前场景化选购链路存在一个 P0 级体验问题：用户提出明显的场景化问题时，系统仍可能退化成普通筛选或普通澄清。

典型例子：

> 母亲节来了，送母亲什么礼物好？

当前实际表现：

1. 后端先返回普通 category 澄清：`你想买哪一类商品？`
2. 用户点击 `美妆护肤` 后，Android 用户气泡显示成 `我是美妆护肤肌肤`
3. 第二轮推荐退化成普通美妆筛选：`我先按美妆护肤这几个条件找一组候选`

这和场景化选购助手的目标冲突。用户不是在问“商品库有哪些品类”，而是在问“这个场景下什么方向更稳、更不容易踩雷”。

---

## 2. 解决方案

P0 只做最小闭环修复：

1. 强场景化请求不再被普通 `category` slot check 截断。
2. 后端自动选择一个当前商品库可支撑的主方向。
3. 场景化链路先用 `text_delta` 输出选购思路，再输出商品候选。
4. Android 澄清选项用户气泡必须按 `required_slots` 生成，不能靠选项文案猜测。
5. 将手动发现的场景化路由问题写入 `scripts/stress_test.py` 回归覆盖。

P0 不做：

- 不做换方向。
- 不做多方向选择。
- 不新增 `ChatStreamRequest` 字段。
- 不新增 SSE event type。
- 不新增复杂策略卡片。

---

## 3. 用户故事

1. 作为用户，我希望“母亲节送母亲什么礼物好”被识别为送礼场景，而不是被要求先选择商品品类，这样 AI 能先帮我判断什么方向更稳。
2. 作为用户，我希望 AI 在展示商品前先解释为什么这个方向适合当前场景，这样推荐更像选购建议，而不是关键词筛选结果。
3. 作为用户，当我说“母亲节送妈妈护肤品怎么选”时，我希望系统保留送礼上下文，而不是只做普通美妆筛选。
4. 作为用户，当我说“推荐敏感肌可用的防晒霜，300元以内”时，我希望它保持普通筛选，不要被误判成场景化选购。
5. 作为用户，当我点击“美妆护肤”这类澄清选项时，我希望聊天气泡自然可读，不要出现“我是美妆护肤肌肤”这类错误文案。
6. 作为开发者，我希望这些手动发现的问题进入压力测试和回归测试，避免后续路由调整再次引入同类问题。

---

## 4. P0 路由设计

### 4.1 当前失败链路

当前顺序：

```text
intent_analysis
→ slot_check
→ handler dispatch
→ 在推荐处理器里检测 shopping_strategy
```

问题：

也就是说，`slot_check` 看到 `category = null` 后，会先发出 `required_slots=["category"]` 的普通澄清，导致 `shopping_strategy` 没有机会执行。

### 4.2 P0 目标顺序

目标顺序：

```text
intent_analysis
→ 确定性后处理
→ scenario_route_detection
→ slot_check
→ handler dispatch
```

`scenario_route_detection` 必须发生在普通槽位检查之前。

### 4.3 强场景化规则

满足以下条件时，认为请求是强场景化请求：

- `intent = recommend`
- `scene_type` 属于 `gift`、`interest`、`travel`
- `scene_score >= 4`

P0 中，强场景化请求必须跳过普通“缺品类”澄清。

示例：

```text
母亲节来了，送母亲什么礼物好？
```

期望路由：

```text
scenario_strategy
```

不能路由为：

```text
clarification(required_slots=["category"])
```

### 4.4 场景化筛选规则

如果用户输入同时包含场景和明确商品/品类约束，应走 `scenario_filter`。

示例：

```text
母亲节送妈妈护肤品，怎么选？
```

期望行为：

- 保留明确品类或商品约束。
- 仍然输出场景化选购判断。
- 不退化成普通“美妆护肤筛选”。

---

## 5. 主方向选择算法

P0 只支持自动选择一个主方向。

### 5.1 方向来源

后端应使用确定性方向池 + 检索探测。

LLM 可以解释为什么选择这个方向，但不能自由编造当前商品库不支持的方向。

### 5.2 算法流程

```text
1. 识别 scene_type。
2. 根据 scene_type、收礼人、节日/场合和消息关键词生成候选方向。
3. 将每个方向映射到现有 CriteriaPayload 字段。
4. 对每个方向执行轻量检索探测。
5. 过滤没有商品库支撑的方向。
6. 对可支撑方向排序：
   - 可召回商品数量
   - 场景匹配度
   - 低踩雷程度
   - 作为礼物或场景解决方案的实用性
7. 选择第一名作为 primary_direction。
8. 将 primary_direction.search_strategy 合并到 criteria。
9. 用 text_delta 流式输出场景判断文字。
10. 输出 criteria_card 和 product_card。
```

### 5.3 P0 示例

输入：

```text
母亲节来了，送母亲什么礼物好？
```

期望主方向：

```text
温和、日常、使用门槛低的护肤方向
```

期望解释风格：

```text
母亲节送妈妈，我不建议一上来选太强功效或太依赖个人偏好的东西。
更稳的是走温和、日常、使用门槛低的护肤方向：不容易闲置，也更适合作为一份持续使用的心意。
```

---

## 6. SSE 与 API 契约

P0 不新增请求字段，不新增事件类型。

复用现有事件：

```text
text_delta
criteria_card
product_card
done
```

对于场景化 turn：

1. `text_delta` 承载场景化选购判断。
2. `criteria_card.shopping_strategy` 承载结构化策略。
3. `product_card` 承载所选主方向下的候选商品。

期望事件顺序：

```text
thinking
text_delta(场景判断文字)
criteria_card(shopping_strategy)
product_card*
text_delta(可选推荐解释文字)
done
```

P0 不做 `scenario_direction`、`scenario_patch`、`strategy_feedback` 或 `replace_target`。

---

## 7. Android 澄清气泡文案

### 7.1 当前失败表现

Android 当前根据选项文案生成可见用户气泡。

因为 `美妆护肤` 包含 `肤` 字，会被错误当成肤质，显示为：

```text
我是美妆护肤肌肤
```

### 7.2 目标规则

可见用户气泡必须根据 `required_slots` 生成，不能只靠选项文案判断。

映射规则：

| `required_slots` | 选项 | 可见用户气泡 |
| --- | --- | --- |
| `category` | `美妆护肤` | `我想看美妆护肤` |
| `skin_type` | `敏感性` | `我是敏感肌肤` 或同等自然表达 |
| `budget` | `1000-2000元` | `预算大概1000-2000元` |
| 其他 | 任意 | 选项原文或中性自然回答 |

具体文案可以沿用 Android 当前语气，但品类不能被渲染成肤质。

---

## 8. 测试决策

### 8.1 后端回归测试

优先在 chat stream / SSE 事件层测试外部行为。

必测用例：

1. `母亲节来了，送母亲什么礼物好？`
   - 不出现普通品类澄清。
   - 在商品卡前输出场景判断文字。
   - 输出 `criteria_card.shopping_strategy.scene_type = gift`。

2. `母亲节送妈妈护肤品，怎么选？`
   - 走场景化链路或 `scenario_filter`。
   - 保留明确品类或商品约束。
   - 不以普通筛选开场作为主体验。

3. `推荐敏感肌可用的防晒霜，300元以内`
   - 不触发 `shopping_strategy`。
   - 保持普通筛选推荐。

### 8.2 Android 单元测试

测试澄清选项生成的用户气泡：

1. `required_slots=["category"]`，选项 `美妆护肤`：不能生成肤质文案。
2. `required_slots=["skin_type"]`，选项 `敏感性`：生成肤质文案。
3. `required_slots=["budget"]`，选项 `1000-2000元`：生成预算文案。

### 8.3 压力测试

将手动发现的问题加入 `scripts/stress_test.py`：

- `母亲节来了，送母亲什么礼物好？`
- `母亲节送妈妈护肤品，怎么选？`
- `推荐敏感肌可用的防晒霜，300元以内`

压力测试应断言外部可观测事件行为，而不是内部辅助函数名称。

---

## 9. 不在 P0 范围内

以下内容明确不属于 P0：

- 换方向按钮。
- 方向选择 UI。
- 多方向对比。
- 独立的场景方向 SSE 事件。
- 新增请求 schema 字段。
- 基于 `replace_target` 的原地重新生成。
- 复杂策略卡片。
- 超出现有能力的旅行/组合式多品类流程。

---

## 10. 验收标准

- [ ] 无品类的强送礼场景不再询问普通品类澄清。
- [ ] 强送礼场景在商品卡前输出场景判断文字。
- [ ] 有明确品类的送礼场景仍保留场景判断。
- [ ] 普通明确筛选请求不会误触发场景化链路。
- [ ] Android 品类澄清选项不再显示为肤质回答。
- [ ] 回归测试和压力测试包含本次手动发现的问题样例。

---

## 11. 已创建 GitHub 问题单

- [#2](https://github.com/Red-Bean-Bun/BuyPilot-AI/issues/2) 后端场景化路由修复。
- [#3](https://github.com/Red-Bean-Bun/BuyPilot-AI/issues/3) Android 澄清气泡文案修复。
- [#4](https://github.com/Red-Bean-Bun/BuyPilot-AI/issues/4) 压力测试回归覆盖。
