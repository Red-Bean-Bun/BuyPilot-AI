# 场景化选购助手 PRD

**版本**：v1.0（2026-06-05）
**状态**：规划稿（前后端终版）
**来源**：[飞书规划文档](https://xcni3ixh2fyf.feishu.cn/wiki/ITd2wNWpOi2j4EkWNONcXJZenrd)
**关联文档**：`contracts/sse-events.schema.json`、`doc/prd/01-Android前端PRD.md`、`doc/prd/02-后端与AgentPRD.md`

---

## 1. 背景与核心洞察

### 1.1 问题

当前场景化推荐体验像关键词筛选。用户说"男朋友生日，喜欢电子产品"后，系统直接按"数码电子"品类找候选商品，再用"品类匹配""日常使用"等字段化理由解释。

但用户真正的问题不是"帮我筛数码电子商品"，而是：

> 我不确定这个场景下送什么更体面、更有心意、更不容易踩雷。

### 1.2 目标

把场景化推荐升级成 **场景化选购助手**：

> 用户提出一个拿不定主意的购物场景时，AI 先帮用户想清楚应该往哪个方向买，再在当前商品库里给出最接近这个方向的候选。

### 1.3 与现有链路的区别

现有链路：

```
用户场景 → 提取关键词 → 生成筛选条件 → 检索商品 → 展示商品 → 补一段推荐理由
```

目标链路：

```
用户场景 → 判断场景真正难点 → 给 2~3 个选购方向 → 说明为什么某个方向更稳 → 再给商品候选 → 用取舍逻辑说服用户
```

---

## 2. 产品定位

### 2.1 不是场景筛选，而是场景判断

- **错误**：男朋友生日 + 电子产品 → 数码电子 → 耳机、手机、平板
- **正确**：男朋友生日 + 电子产品 → 这是礼物场景 → 收礼人可能更懂核心数码设备 → 手机电脑容易踩型号偏好 → 更稳的是黑科技小件、音频配件、智能穿戴 → 再从商品库找候选

### 2.2 用户价值

- 帮我判断该买什么方向
- 帮我避免显得随便或踩雷
- 帮我解释为什么这个方向更合适
- 如果商品库有限，也要告诉我当前推荐为什么是"有限候选里的稳妥选择"

### 2.3 场景类型（P0 优先支持高频、高 LLM 价值场景）

| 场景类型 | 用户表达示例 | 决策重点 |
|---------|------------|---------|
| 送礼 | 男朋友生日、送朋友、送妈妈 | 体面、心意、低踩雷、预算 |
| 兴趣导向 | 喜欢足球、喜欢电子产品、爱拍照 | 兴趣相关但不过度冒险 |
| 使用场景 | 通勤、上班、旅行、日常训练 | 实用性、稳定性、便携性 |
| 风险敏感 | 敏感肌、怕踩雷、送人不知道喜好 | 风险规避、低刺激、低偏好依赖 |
| 目标导向 | 拍照多、打游戏、控油、无糖 | 目标维度权重 |

P0 只实现 **送礼** 和 **兴趣导向** 两类场景。其余 P1 扩展。

### 2.4 抖音电商转化定位

在抖音电商场景中，场景化选购助手不是"聊天版搜索框"，而是 **内容种草后的购买决策转化层**：

```
短视频/直播/搜索激发兴趣 → 用户产生犹豫 → AI 识别购买阻力
→ 给出低风险选购方向 → 用证据和取舍逻辑降低顾虑 → 推进到对比/查看依据/加购
```

核心业务价值：
- 把"被内容种草但不知道该不该买"的用户推进到可执行购买动作
- 把模糊兴趣转成结构化购买标准，提升商品点击和加购效率
- 通过风险前置减少不适配成交，提升有效 GMV，降低退货/差评/客服成本

P0 不接入真实短视频、直播、优惠券或达人数据，但在产品逻辑上预留"种草后转化"叙事：每次场景化推荐都必须回答 **用户为什么还没下单**，并给出最合适的下一步动作。

---

## 3. 协议与数据模型

### 3.1 场景化能力不新增 SSE event type

SSE event 全集以 `contracts/sse-events.schema.json` 为准（当前 10 种：thinking / clarification / criteria_card / text_delta / product_card / cart_action / final_decision / done / error / compare_card）。场景化选购**不在此基础上新增 event type**，复用现有通道：

| 承载通道 | 用途 |
|---------|------|
| `text_delta` | 输出 1~2 句场景判断文案 |
| `criteria_card.shopping_strategy` | `criteria_card` 上新增**可选**字段，渲染"选购思路区" |

好处：不破坏现有 Android SSE parser、普通推荐链路不受影响、老版本前端忽略未知字段。

### 3.2 ShoppingStrategyPayload

`shopping_strategy` 是 `criteria_card` 的**可选顶层字段**（与 `criteria` 平级，不是其子字段）。

```json
{
  "event": "criteria_card",
  "display_mode": "summary_card",
  "editable": true,
  "criteria": { "..." : "..." },
  "shopping_strategy": {
    "strategy_id": "scene_001",
    "scene_type": "gift",
    "scene_summary": "送男朋友生日礼物，对方喜欢电子产品",
    "user_problem": "用户不确定这个场景下送什么更体面、更有心意、更不容易踩雷",
    "decision_barrier": {
      "barrier_type": "fear_wrong_choice",
      "label": "怕送错、怕不够体面",
      "reason": "对方懂电子产品，核心设备容易踩型号和品牌偏好",
      "conversion_strategy": "先推荐低偏好依赖、礼物感更强的小件，并保留查看依据和换方向入口"
    },
    "primary_direction": {
      "title": "低踩雷的黑科技小件",
      "summary": "优先考虑音频配件、智能穿戴或轻量数码配件",
      "why": "有新鲜感，不强依赖具体型号偏好，也更适合生日礼物",
      "search_strategy": {
        "category": "数码电子",
        "product_type": "真无线耳机",
        "use_scenario": "日常使用"
      },
      "available_in_catalog": true,
      "supporting_product_count": 2
    },
    "avoid_risks": [
      "不要盲买手机、电脑这类强型号偏好的大件",
      "不知道常用品牌时，优先选兼容性更强的小件"
    ],
    "assumptions": [
      "暂时不知道预算",
      "暂时不知道对方常用品牌"
    ],
    "confidence": "medium"
  },
  "quick_actions": []
}
```

### 3.3 字段约束

| 字段 | 类型 | 说明 |
|------|------|------|
| `strategy_id` | `string` | 本次场景策略 ID，便于 trace 和 final_decision 引用 |
| `scene_type` | `enum` | `gift` / `interest` / `usage` / `risk_sensitive` / `goal_oriented` |
| `scene_summary` | `string` | 对用户场景的压缩描述，不重复原话 |
| `user_problem` | `string` | 用户真实要解决的问题 |
| `decision_barrier` | `object?` | 用户当前最主要的购买阻力，P0 可选但推荐输出 |
| `primary_direction` | `object` | 当前商品库可落地的主选购方向（见下表） |
| `avoid_risks` | `string[]` | 场景前置避坑点，不得包含商品库外商品事实 |
| `assumptions` | `string[]` | 因缺少预算/品牌/肤质等信息而采用的默认假设 |
| `confidence` | `enum` | `low` / `medium` / `high` |

`decision_barrier` 子字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `barrier_type` | `enum` | `fear_wrong_choice` / `value_uncertainty` / `fit_uncertainty` / `trust_uncertainty` / `price_sensitive` / `choice_overload` |
| `label` | `string` | 面向用户可读的阻力描述 |
| `reason` | `string` | 为什么该场景下会产生这个阻力 |
| `conversion_strategy` | `string` | 本轮如何降低阻力并推进下一步动作 |

`primary_direction` 子字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | `string` | 主方向名称 |
| `summary` | `string` | 方向说明 |
| `why` | `string` | 为什么这个方向更稳 |
| `search_strategy` | `object` | 必须能映射到现有 `CriteriaPayload.constraints` |
| `available_in_catalog` | `boolean` | 该方向是否被当前商品库支撑 |
| `supporting_product_count` | `int` | 可召回商品数，用于可信度和降级 |

### 3.4 与 CriteriaPayload 的关系

- `shopping_strategy`：解释"这个场景下为什么先往这个方向买"
- `criteria`：承接方向，告诉系统"实际按什么条件检索"

P0 要求 `primary_direction.search_strategy` 必须能降维到现有 `criteria.constraints`。多方向并行检索放到 P1。

### 3.5 Schema 变更

**文件**：`contracts/sse-events.schema.json`

在 `$defs.CriteriaCardPayload` 的 `properties` 中新增：

```json
"shopping_strategy": { "$ref": "#/$defs/shopping_strategy_payload" }
```

新增 `$defs/shopping_strategy_payload` 和 `$defs/primary_direction_payload` 定义。`shopping_strategy` 为可选字段，前端不解析时不影响现有逻辑。

### 3.6 Replace Target（P1）

> **契约前置**：`replace_target` 是 P1 方向反馈替换流的核心机制。实现前必须同步完成以下变更，否则原地替换只能停留在 PRD 文案：
> 1. `contracts/sse-events.schema.json` — 在 `envelope` 或各事件的 `properties` 中新增可选 `replace_target` 字段定义
> 2. `backend/src/types/sse_events.py` — `SSEEventBase` 新增 `replace_target: ReplaceTargetPayload | None = None`
> 3. Android `ChatReducer.kt` — 新增 group replace 语义（按 `replace_target.group_id` 替换同一 turn 内的旧节点组）

方向反馈替换流中，现有事件可携带可选 `replace_target` 字段：

```json
{
  "replace_target": {
    "turn_id": "turn_xxx",
    "group_id": "strategy_group_001",
    "mode": "replace"
  }
}
```

适用事件：`text_delta`、`criteria_card`、`product_card`、`done`。

---

## 4. 场景化选购算法

### 4.1 总体流程

```
用户输入 → 场景触发判断 → 构建购物任务框架 → 生成候选选购方向 → 商品库可行性检查
→ 选择主方向 → 映射到 CriteriaPayload → 检索商品 → 生成说服式推荐文案
→ 输出 text_delta + criteria_card(shopping_strategy) + product_card
```

### 4.2 Step 1：首轮路由判断

输入：当前 message、会话历史摘要、当前模式（是否 Swipe/cart/compare/final_decision 后追问）、已识别的 category/product_type/budget/品牌。

输出：

```json
{
  "route": "scenario_strategy",
  "should_use_shopping_strategy": true,
  "scene_type": "gift",
  "scene_score": 7,
  "filter_score": 0,
  "confidence": "medium",
  "reason": "用户表达送礼场景，并且在问应该送什么"
}
```

#### 路由规则

| Route | 含义 | 是否输出 shopping_strategy |
|-------|------|--------------------------|
| `filter_recommend` | 普通筛选推荐 | 否 |
| `scenario_strategy` | 场景化选购 | 是 |
| `scenario_filter` | 混合：既有场景也有明确筛选 | 是，但必须尊重明确筛选 |
| `clarification` | 信息不足 | 否，先问澄清 |
| `broad_recommend` | 模糊推荐 | 否 |

#### 评分规则

**scene_score**：

| 信号 | 示例 | 分值 |
|------|------|------|
| 有对象 | 男朋友、妈妈、朋友 | +2 |
| 有场合 | 生日、送礼、旅行、通勤 | +2 |
| 不确定表达 | 应该买什么、怎么选 | +2 |
| 风险表达 | 怕踩雷、不知道喜好、敏感肌 | +2 |
| 兴趣画像 | 喜欢电子产品、爱拍照 | +1 |

**filter_score**：

| 信号 | 示例 | 分值 |
|------|------|------|
| 明确商品类型 | 蓝牙耳机、防晒霜 | +3 |
| 明确预算 | 2000 内、300 元左右 | +2 |
| 明确硬参数 | 256G、无酒精、控油 | +2 |
| 明确品牌 | 小米、OPPO | +2 |
| 明确排序目标 | 性价比最高、最便宜 | +1 |

#### P0 判定逻辑

```
if 当前处于 Swipe 收敛 / cart / compare / final_decision 后动作:
    不进入场景化路由，交给对应 action handler

# P0 场景类型硬门控：只有 gift / interest 进入场景化
else if scene_type not in {gift, interest}:
    # 即使 scene_score >= 4，P0 不输出 shopping_strategy
    # 降级为 filter_recommend 或 clarification
    route = filter_recommend 或 clarification

else if scene_score >= 4 and filter_score <= 3:
    route = scenario_strategy

else if filter_score >= 5 and scene_score <= 2:
    route = filter_recommend

else if scene_score >= 4 and filter_score >= 4:
    route = scenario_filter

else:
    route = clarification 或 broad_recommend
```

#### 路由示例

| 用户输入 | scene_score | filter_score | route |
|---------|-------------|-------------|-------|
| "男朋友过生日，喜欢电子产品，应该送什么好？" | 7 | 0 | `scenario_strategy` |
| "2000 内蓝牙耳机有哪些？" | 0 | 5 | `filter_recommend` |
| "送男朋友一个 2000 内蓝牙耳机，怎么选？" | 6 | 5 | `scenario_filter` |
| "我想买个手机，平时拍照多。" | 1 | 4 | `filter_recommend` |
| "想买点东西，有推荐吗？" | 0 | 0 | `clarification` / `broad_recommend` |

#### scenario_filter 特殊规则

- 输出轻量 `shopping_strategy`
- **不能**改变用户明确商品类型（用户说蓝牙耳机就只能在蓝牙耳机内推荐）
- **不能**覆盖预算、品牌、参数等显式约束
- UI 上选购思路区更短

### 4.3 Step 2：构建购物任务框架

```json
{
  "scene_type": "gift",
  "recipient": "男朋友",
  "occasion": "生日",
  "interest": "电子产品",
  "explicit_category": null,
  "category_hint": "数码电子",
  "explicit_constraints": { "budget_max": null, "brand_prefer": [] },
  "decision_problem": "不知道该送什么方向更稳",
  "decision_barrier": {
    "barrier_type": "fear_wrong_choice",
    "label": "怕送错、怕不够体面"
  },
  "risk_focus": ["强型号偏好", "重复购买", "礼物感不足"],
  "missing_slots": ["budget", "brand_prefer"]
}
```

规则：
- 用户明说的预算、品牌、肤质等进入 `explicit_constraints`，不得被后续策略覆盖
- 没明说的信息只能进入 `missing_slots` 或 `assumptions`
- `category_hint` 可由 LLM 推断，但必须能映射到四大品类之一

### 4.4 Step 3：生成候选选购方向

由 `ShoppingStrategyService` 调用 LLM 生成 2~4 个候选方向。

输入：`ShoppingTaskFrame` + 商品库可用类目摘要 + 现有 constraints DSL

输出示例：

```json
[
  {
    "title": "低踩雷的黑科技小件",
    "summary": "优先考虑音频配件或智能穿戴",
    "why": "有数码感和新鲜感，但不强依赖具体型号偏好",
    "search_strategy": {
      "category": "数码电子",
      "product_type": "真无线耳机",
      "use_scenario": "日常使用"
    },
    "avoid_risks": ["手机、电脑这类核心设备容易买错型号"],
    "assumptions": ["暂时不知道预算和常用品牌"]
  }
]
```

LLM 输出硬约束：
- 不允许出现当前商品库没有的确定商品名
- `search_strategy.category` 必须在四大品类内
- `search_strategy.product_type` P0 必须是单值
- 每个方向必须包含 `why` 和至少 1 条 `avoid_risks`
- 不得使用"最优解""一定适合""绝对不会踩雷"等过度承诺

### 4.5 Step 4：商品库可行性检查

每个候选方向做检索探测。**P0 和 P1 使用不同评分粒度**：

#### P0 轻量评分（仅必做项）

```
feasibility_score_p0 = 0.50 * retrieval_coverage
                     + 0.30 * category_match
                     + 0.20 * constraint_safety
```

| 项 | 计算方式 |
|---|---------|
| `retrieval_coverage` | `min(count, 3) / 3` |
| `category_match` | 方向 category 与召回商品 category 的一致率 |
| `constraint_safety` | 是否违反预算、肤质、忌口等显式硬约束（违反 = 0，通过 = 1） |

#### P1 完整评分（加入 evidence_quality 和多方向排序）

```
feasibility_score_p1 = 0.45 * retrieval_coverage
                     + 0.25 * category_match
                     + 0.20 * evidence_quality
                     + 0.10 * constraint_safety
```

| 项 | 计算方式 |
|---|---------|
| `evidence_quality` | 召回商品是否有 marketing/FAQ/review chunk |

P0 不计算 `evidence_quality`，不在多候选方向间做排序，仅判断"是否可落地"。

方向状态：

| 条件 | 状态 | 行为 |
|------|------|------|
| `count >= 2` 且 `score >= 0.55` | `supported` | 可作为主方向 |
| `count == 1` 且 `score >= 0.45` | `weak_supported` | 可作为降级方向 |
| `count == 0` 或违反硬约束 | `unsupported` | 不展示，不作为主方向 |

所有方向都不支持时：先用更宽 `category_hint` 重新检索一次；仍无结果则发 `clarification` 或普通无结果说明；不展示"选购思路区"。

### 4.6 Step 5：选择主方向并映射 criteria

主方向优先级：
1. `feasibility_score` 最高
2. 不违反用户显式约束
3. 能解释用户场景风险

映射规则：

| `search_strategy` | `CriteriaPayload` |
|-------------------|-------------------|
| `category` | `criteria.category` |
| `product_type` | `criteria.constraints.product_type` |
| `use_scenario` | `criteria.constraints.use_scenario` |
| 用户显式预算 | `criteria.constraints.budget_min/budget_max` |
| 用户显式品牌偏好 | `brand_prefer/brand_avoid` |
| 用户显式肤质 | `skin_type` |

P0 不允许 `shopping_strategy` 覆盖用户显式 constraints。

#### 显式约束保留规则（铁律）

**只要用户显式给出以下任一维度，所有路由（含 `scenario_strategy`）都必须保留，不得被策略覆盖：**

| 维度 | 示例 |
|------|------|
| `product_type` / `category` | "蓝牙耳机""护肤品""手机" |
| `budget` | "200 内""预算 3000" |
| `brand` | "小米""OPPO" |
| 硬参数 | "256G""无酒精""控油" |

违反此规则即架构错误。场景化只能在用户**未指定**的维度上提供方向建议。

### 4.7 Step 6：生成对用户可见文案

三类文案：

1. **text_delta 场景判断**（1~2 句）：
   > 这不是单纯买数码产品，而是送懂数码的男朋友生日礼物。核心设备他往往更懂也更挑，所以我会先避开手机、电脑这类强型号偏好的大件。

2. **criteria_card.shopping_strategy 结构化选购思路**：
   > 选购思路：低踩雷的黑科技小件
   > 为什么：有新鲜感，不强依赖具体型号偏好，也更适合生日礼物。

3. **商品推荐文案**：
   > 我先按这个方向从当前商品库里找。耳机这类小件比手机更稳，生日礼物感也比普通配件强。

文案规则：
- 使用"更稳""更适合当前商品库"，不用"最佳""最优解"
- 商品事实必须来自 `product_card`、`reason_atoms` 或 evidence
- 前端不得本地生成策略文案
- 商品库支撑弱时必须说"当前商品库里更接近的是..."
- 必须显式回应 `decision_barrier`，即解释本轮推荐如何降低用户的购买顾虑

### 4.8 Step 7：转化动作选择（Next Best Action）

场景化推荐不只输出商品，还要根据 `decision_barrier` 选择最合适的下一步动作。P0 不新增 quick action 类型，复用现有 `quick_action_payload.action`：

| `decision_barrier.barrier_type` | 用户心理 | 推荐动作 | 承载位置 |
|---------------------------------|----------|----------|----------|
| `trust_uncertainty` | 不确定推荐是否可信 | `open_evidence` / 查看依据 | `product_card.actions` |
| `choice_overload` | 候选太多，不知道选哪个 | `compare` / 帮我对比 | `final_decision.next_actions` 或商品区入口 |
| `price_sensitive` | 觉得价格可能偏高 | `criteria_patch` / 换更低预算 | `criteria_card.quick_actions`、`final_decision.next_actions` |
| `fear_wrong_choice` | 怕送错、怕踩雷 | `add_to_cart` 或继续对比 | `final_decision.next_actions` |
| `fit_uncertainty` | 担心不适合肤质/场景/尺码 | `open_evidence` / 查看风险说明 | `product_card.actions` |

P0 必做：
- 商品卡保留 `查看依据`、`加入购物车`、`不喜欢这个`
- `final_decision.decision_status == selected` 时，`next_actions` 必须包含 `加入购物车`
- 如果 `confidence != high`，优先给 `帮我对比` 或 `查看依据`，不强推加购
- P0 所有 CTA 都必须能落到现有 action，不新增任何 quick action 类型；`strategy_feedback` 仍仅属于 P1

业务指标映射：
- `open_evidence` 提升信任和商品详情点击
- `compare` 降低选择困难，推动决策收敛
- `criteria_patch` 承接价格敏感用户，减少直接流失
- `add_to_cart` 把最终决策转成明确加购动作

### 4.9 Step 8：商品理由改写

商品卡 `reason` 从字段匹配变成场景价值：

- **错误**：真无线耳机品类匹配。
- **正确**：比手机更低踩雷，适合作为数码生日小礼物。

每个商品 reason ≤ 28 中文字符；不出现"品类匹配""根据您的需求"；不编造商品库外对比对象。商品 reason 要优先服务转化阻力：

| 阻力 | reason 写法 |
|------|-------------|
| 怕送错 | "低偏好依赖，送礼更稳" |
| 怕不值 | "核心功能集中，预算更可控" |
| 怕不适合 | "风险点清楚，适合再核对" |
| 选择困难 | "礼物感和实用性更平衡" |

---

## 5. 方向反馈与原地替换（P1）

> **P0 不实现本章节任何内容。** P0 不下发 `strategy_feedback` quick action，不展示"换个思路"按钮，不实现原地替换。以下内容均为 P1 规划。

### 5.1 Quick Action：strategy_feedback

P1 需在 `contracts/sse-events.schema.json` 的 `quick_action_payload.action` enum 中新增 `"strategy_feedback"`，当前 enum 仅含 `criteria_patch` / `feedback` / `open_evidence` / `compare` / `add_to_cart`。新增时同步定义：
- 请求模型（`strategy_feedback` 字段结构）
- 失败语义（后端未实现或失败时不返回该 action）
- Android 侧 `AgentEventType` 和 reducer 对应处理

按钮入口文案：`换个思路`

点击规则：
- 不显示用户气泡
- 不新增 assistant 回复
- 原地替换当前 assistant turn 中的场景化内容
- 请求仍走 `/chat/stream`
- 成功后才写入后端反馈历史；失败不记录

### 5.2 请求 payload

```json
{
  "action": "strategy_feedback",
  "strategy_feedback": {
    "strategy_id": "scene_001",
    "feedback_type": "reject_direction",
    "rejected_direction_id": "direction_low_risk_gadget",
    "keep_scene": true,
    "replace_in_place": true,
    "target_turn_id": "turn_xxx",
    "target_group_id": "strategy_group_001"
  }
}
```

### 5.3 自动换方向次数

- 第 1 次：换到第二个 supported direction
- 第 2 次：换到第三个 supported direction 或更宽 fallback
- 第 3 次：不再自动换，提示"暂时没有更稳的方向" + `补充偏好` 按钮

### 5.4 撤回

换方向成功后显示轻量撤回入口。撤回走后端，返回上一次 snapshot，不重新生成。

```json
{
  "strategy_feedback": {
    "feedback_type": "undo_last_strategy_feedback",
    "target_feedback_id": "sf_001",
    "replace_in_place": true,
    "target_turn_id": "turn_xxx",
    "target_group_id": "strategy_group_001"
  }
}
```

### 5.5 Strategy Snapshot 与候选方向存储

后端需保存 strategy snapshot 用于原地替换和撤回。每次成功的 strategy_feedback 产生一个新 snapshot，包含 criteria、shopping_strategy、products、文案等完整状态。

#### 候选方向存储方案

LLM 生成的 2~4 个候选方向**仅存后端**，不通过 SSE 下发给前端：

```json
{
  "strategy_snapshot": {
    "strategy_id": "scene_001",
    "candidate_directions": [
      {"direction_id": "dir_1", "title": "...", "feasibility": "supported", "...": "..."},
      {"direction_id": "dir_2", "title": "...", "feasibility": "supported", "...": "..."}
    ],
    "active_direction_id": "dir_1",
    "rejected_direction_ids": []
  }
}
```

P1"换个思路"时：
1. 从 `candidate_directions` 中取下一个 `supported` 且未被 reject 的方向
2. 更新 `active_direction_id`
3. 重新检索商品，生成新 snapshot
4. **不重新调用 LLM**（除非所有候选都已 reject，此时才重新生成）

这样既避免 SSE payload 膨胀，又保证"换个思路"的响应速度。

### 5.6 原地替换范围

替换范围：scenario_judgement_text + shopping_strategy + product_cards + recommendation_text + criteria_secondary

不替换：用户气泡、之前的历史回复、已独立产生的 final_decision、用户手动点过的详情页状态。

---

## 6. SSE 顺序

> **关键约束**：以下两种顺序**互斥**，由 `shopping_strategy` 是否非空决定。普通推荐链路的 SSE 顺序不受影响，现有测试断言保持不变。

### 6.1 场景化推荐（仅当 `shopping_strategy != null`）

```
thinking: 正在判断这个场景怎么买更稳
text_delta: 场景判断 1~2 句
criteria_card: criteria + shopping_strategy（shopping_strategy 非空）
thinking: 正在按这个方向查找候选
product_card*
text_delta: 说服式推荐文字
done
```

**仅当该 turn 携带 `shopping_strategy` 时，商品不得早于场景判断和选购思路区出现。** 普通推荐 turn 不在此约束范围内。

### 6.2 非场景化普通推荐（`shopping_strategy == null` 或无此字段）

保持原链路不变，现有测试断言的 product-first 顺序不受影响：

```
thinking → product_card* → criteria_card → text_delta → done
```

### 6.3 方向反馈替换流（P1）

```
request: strategy_feedback
text_delta: replace_target + 新 scenario_judgement_text
criteria_card: replace_target + criteria + shopping_strategy
product_card*: replace_target + 新候选商品
text_delta: replace_target + 新 recommendation_text
done: replace_target
```

所有可见事件都带同一个 `replace_target.group_id`。

### 6.4 契约测试影响

现有测试（如 `test_pipeline.py`、`test_viewmodel_contract.py`）断言普通推荐的 product_card / criteria_card 顺序。场景化链路引入新顺序后，必须：
- **不修改**现有普通推荐测试的断言
- **新增**条件化契约测试：仅当 `shopping_strategy != null` 时断言 text_delta 先于 product_card
- 测试用例中标注"场景化 turn"和"普通 turn"，两组互不干扰

---

## 7. 后端实现规范

### 7.1 新增模块

| 模块 | 文件 | 职责 |
|------|------|------|
| `ShoppingStrategyService` | `services/shopping_strategy.py` | 场景触发判断、任务框架、候选方向、可行性检查、策略 payload 组装 |
| `shopping_strategy.md` | `prompts/shopping_strategy.md` | 约束 LLM 输出候选选购方向 |
| `CriteriaCardEvent.shopping_strategy` | `types/sse_events.py` | 可选字段，承载结构化选购思路 |

P0 不拆 `ScenarioClassifier` / `ShoppingStrategyPlanner` / `ScenarioFeasibilityChecker` 为独立类，作为 `ShoppingStrategyService` 内部函数。

### 7.2 LLM 的角色

LLM 负责：
- 理解场景背后的真实购买任务
- 推断隐含风险
- 生成选购方向和解释
- 把结构化结果写成人话

LLM 不负责：
- 编造当前商品库没有的商品事实
- 绕过检索直接决定某个商品一定最好
- 修改用户显式约束
- 生成不可验证的优惠、库存、销量结论

### 7.3 Prompt 规划

新增 `prompts/shopping_strategy.md`，约束：
1. 输出 JSON，不输出自由散文
2. 先识别 `scene_type` 和 `decision_problem`
3. 生成 2~4 个候选方向
4. 每个方向必须包含 `search_strategy`
5. 每个方向必须说明 `why` 和 `avoid_risks`
6. 不得编造当前商品库没有的商品名、价格、优惠、库存
7. 不要使用"根据您的需求""为您推荐"等客服话术
8. 不要输出"最佳""最优""绝对适合"等强承诺

### 7.4 与现有推荐链路的关系

场景化链路是推荐链路上的可选前置策略阶段：

```
intent → scenario strategy gate → shopping task frame（可选）
→ candidate directions（可选）→ feasibility check（可选）
→ criteria → retrieval → product_card → recommendation text
```

Runtime 只做编排；触发判断、候选方向、可行性检查放在 Service 层。

### 7.5 Pipeline 集成

在 `pipeline.py` 中：
- 意图为 `recommend` 时，进入 handler 前先调场景路由判断
- 路由结果为 `scenario_strategy` 或 `scenario_filter` 时，注入 shopping_strategy 阶段
- 路由结果为 `filter_recommend` / `clarification` / `broad_recommend` 时，走现有链路

---

## 8. 前端实现规范

### 8.1 数据解析

Android 不新增 event type。在 `CriteriaCardPayload` 增加可选字段：

- `shoppingStrategy: ShoppingStrategyPayload?`
- `ScenarioDirectionPayload`
- `SearchStrategyPayload`

SSE parser 仍按 `AgentEventType.CriteriaCard` 分发。

### 8.2 选购思路区

有 `shopping_strategy` 时渲染内嵌区块：

```
选购思路
懂电子产品的人，核心设备别乱买

优先方向
低踩雷的黑科技小件

为什么
有新鲜感，不强依赖具体型号偏好，也更适合生日礼物。
```

字段映射：

| UI | 数据 |
|----|------|
| 标题 | 固定"选购思路" |
| 一句话判断 | `user_problem` |
| 购买顾虑 | `decision_barrier.label` |
| 主方向 | `primary_direction.title` |
| 主方向说明 | `primary_direction.summary` |
| 为什么 | `primary_direction.why` |
| 避坑点 | `avoid_risks` |
| 默认假设 | `assumptions` |

视觉要求：
- 不做独立卡片，作为 assistant 回复内的轻量内嵌区块
- 用细分割线、局部浅底或小标题建立层级
- 不像筛选卡，也不像警告卡
- coral 只用于极小状态点或主方向强调
- 筛选条件放在选购思路区底部的折叠/次级区域

### 8.3 方向反馈按钮（P1）

选购思路区末尾保留轻量按钮：`换个思路`

| 状态 | 文案 | 行为 |
|------|------|------|
| 默认 | `换个思路` | 发送 `strategy_feedback.reject_direction` |
| 请求中 | `换思路中...` | 禁用重复点击 |
| 成功 | `已换成新的思路` + `撤回` | 显示短暂状态和撤回入口 |
| 失败 | `没换成功，再试一次` | 保留旧内容，允许重试 |
| 无替代方向 | `暂时没有更稳的方向` + `补充偏好` | 聚焦输入框 |

### 8.4 原地替换动效（P1）

| 元素 | 动效 |
|------|------|
| 旧 group | opacity 1→0，160~220ms |
| 新 group | fade + 上移 8dp，220~280ms |
| 商品卡 | 跟随新 group，轻微 stagger |
| reduce motion | 直接替换，不播放 stagger |

### 8.5 Timeline 规则

| 条件 | 渲染 |
|------|------|
| `criteria_card.shopping_strategy != null` | 渲染内嵌"选购思路区"，筛选条件降级为轻量区域 |
| `criteria_card.shopping_strategy == null` | 保持现有筛选卡 |

---

## 9. 小数据策略

### 9.1 只推荐库里能支撑的方向

如果库里没有球队周边，不要说"我推荐球队周边"。

可以说：
> 如果你想更有纪念感，球队周边会更强；但当前商品库更适合先按运动休闲礼物方向找。

### 9.2 把有限性变成可信感

不说：`这是最佳足球礼物。`

说：
> 当前候选里更接近的是运动休闲单品。它不强绑定球队偏好，日常能穿，作为稳妥礼物比随机足球装备更低风险。

### 9.3 不做虚假方向

`available_in_catalog: false` 的方向 P0 前端隐藏。

---

## 10. 和现有功能的关系

### 10.1 和 criteria_card

`shopping_strategy` 是"为什么这样买"，`criteria_card` 是"按什么条件找"。二者共用同一个 SSE event，UI 层级区分：选购思路是主叙事，筛选条件是次级说明。

### 10.2 和 compare 模式

场景化给方向，对比模式做取舍：

```
男朋友生日，喜欢电子产品
→ 选购思路：低踩雷黑科技小件
→ 候选：耳机、智能穿戴、轻量数码配件
→ 对比：这几款哪个更适合送礼
→ 结论：选耳机，因为新鲜感和实用性更平衡
```

### 10.3 和 final_decision

final_decision 要引用场景判断：

- **错误**：综合来看，推荐华为 FreeBuds。
- **正确**：如果这次是送懂电子产品的男朋友生日礼物，FreeBuds 更稳：它有数码感，也不像手机那样依赖具体型号偏好。

如果 `decision_status == selected`：
- `final_decision.next_actions` 必须包含 `加入购物车`
- 前端点击后复用现有 `add_to_cart` quick action 逻辑，不显示新的用户气泡
- 如果候选置信度不足，则 `加入购物车` 可以排在 `帮我对比` 之后，但不能缺失

### 10.4 和转化指标

场景化选购助手的业务目标不是简单增加回复长度，而是提升"种草后转化效率"。PRD 中每个转化动作对应可解释 KPI：

| 产品机制 | 对应 KPI |
|----------|----------|
| `decision_barrier` | 购买阻力识别率、有效推荐率 |
| `open_evidence` | 证据打开率、商品详情点击率 |
| `compare` | 决策收敛率、继续交互率 |
| `criteria_patch` | 价格敏感用户留存率 |
| `add_to_cart` | 加购率、加购转化时长 |
| 风险前置 `risk_notes` | 有效成交率、退款/差评风险下降 |

P0 不做完整数据看板，但 trace 中应能复盘：本轮识别了什么购买阻力、给了什么 CTA、用户是否点击了证据/对比/加购。

---

## 11. 实施计划

### 11.1 P0：先做可录屏效果

**后端**：
- [ ] 新增 `prompts/shopping_strategy.md`
- [ ] 新增 `services/shopping_strategy.py`（`ShoppingStrategyService`）
- [ ] 在推荐 pipeline 中加入可选 shopping strategy 阶段
- [ ] 实现首轮路由判断（`filter_recommend` / `scenario_strategy` / `scenario_filter`）
- [ ] 支持送礼和兴趣导向两类场景
- [ ] **轻量可行性检查**：每个候选方向做检索探测（count、category_match、显式硬约束），`unsupported` 方向不作为主方向、`available_in_catalog=false`
- [ ] `CriteriaCardEvent` 输出可选 `shopping_strategy` 字段
- [ ] `ShoppingStrategyPayload` 输出可选 `decision_barrier`
- [ ] recommendation text 接收 `shopping_strategy` 上下文
- [ ] 商品 reason 接收 `primary_direction` + `avoid_risks`
- [ ] 商品 reason 显式服务 `decision_barrier`
- [ ] `final_decision.decision_status == selected` 时，下发 `add_to_cart` next action
- [ ] P0 **不下发** `strategy_feedback` quick action（按钮不展示，避免空操作）

**前端**：
- [ ] 在 `CriteriaCardPayload` 解析 `shopping_strategy`
- [ ] 有 `shopping_strategy` 时渲染选购思路区，并展示购买顾虑（`decision_barrier.label`）
- [ ] P0 不展示 `换个思路` 按钮（后端未接通策略反馈逻辑）
- [ ] 调整场景化时的卡片顺序（选购思路在商品前）
- [ ] 商品卡短理由优先使用后端场景理由
- [ ] final_decision 的 `加入购物车` 复用现有 `add_to_cart` quick action，不新增用户气泡

**验收**：
- "男朋友生日，喜欢电子产品"先出现选购思路，再出现商品
- "2000 内蓝牙耳机有哪些"仍走普通筛选，不出现选购思路
- "送男朋友一个 2000 内蓝牙耳机，怎么选"走混合模式，检索严格限制在蓝牙耳机
- 回复不再是"按数码电子找候选"
- 商品解释能体现礼物低踩雷逻辑
- 选购思路区能看到用户购买顾虑，例如"怕送错、怕不够体面"
- 最终决策卡出现 `加入购物车` 动作，点击后能完成现有购物车加购链路
- SSE event 全集以 `contracts/sse-events.schema.json` 为准，场景化不新增 event type

### 11.2 P1：更稳定的场景策略

**后端**：
- [ ] 升级可行性检查为完整版（加入 `evidence_quality` 权重和多候选方向排序）
- [ ] 支持旅行、通勤、运动、敏感肌等场景（移除 P0 `scene_type` 硬门控）
- [ ] 完成 `strategy_feedback.reject_direction` + snapshot + undo
- [ ] 无可用新方向时返回 `done.finish_reason=no_alternative_strategy`

**P1 契约变更清单**（实现前必须同步完成）：

| 变更点 | 文件 | 内容 |
|--------|------|------|
| `finish_reason` enum | `contracts/sse-events.schema.json` | `done` 的 `finish_reason` enum 新增 `no_alternative_strategy` |
| `finish_reason` enum | `backend/src/types/sse_events.py` | `DoneEvent.finish_reason` Literal 新增 `no_alternative_strategy` |
| `quick_action` enum | `contracts/sse-events.schema.json` | `quick_action_payload.action` enum 新增 `strategy_feedback` |
| 请求模型 | `backend/src/types/schemas.py` | `ChatStreamRequest` 新增可选 `strategy_feedback: StrategyFeedbackPayload \| None` |
| 请求模型 | `contracts/sse-events.schema.json` | 新增 `strategy_feedback_payload` 定义 |
| `replace_target` | `contracts/sse-events.schema.json` | envelope 或各事件新增可选 `replace_target` 字段 |
| `replace_target` | `backend/src/types/sse_events.py` | `SSEEventBase` 新增 `replace_target: ReplaceTargetPayload \| None = None` |
| Android reducer | `ChatReducer.kt` | 新增 group replace 语义（按 `replace_target.group_id` 替换） |

**前端**：
- [ ] `换个思路` / `撤回` / `补充偏好` 原地替换当前 group
- [ ] 替换时旧内容保留到首个有效 `replace_target` 事件到达后再淡出
- [ ] 详情页展示场景理由

### 11.3 P2：和对比、收敛打通

- [ ] `shopping_strategy` 进入 compare focus
- [ ] final_decision 引用 `shopping_strategy`
- [ ] 如确需独立事件，再通过契约升级新增 `scenario_advice` event

---

## 12. 测试用例

### 12.1 后端测试

**P0 必测**（送礼 + 兴趣导向）：
1. "男朋友生日，喜欢电子产品"触发 `shopping_strategy`
2. "2000 内蓝牙耳机有哪些"不触发 `shopping_strategy`
3. "送男朋友一个 2000 内蓝牙耳机，怎么选"触发 `scenario_filter`，且 criteria 保留 `product_type=蓝牙耳机`
4. `shopping_strategy` 输出 `scene_type`、`primary_direction`、`avoid_risks`
5. 输出 search_strategy 后，criteria 能承接该方向
6. 轻量可行性检查：商品库不支持的方向（count=0）不作为主方向，`available_in_catalog=false`
7. recommendation text 能引用 primary_direction
8. 场景化推荐不新增 event type（全集以 schema 为准）
9. 用户显式 constraints 不会被策略覆盖
10. `shopping_strategy.decision_barrier` 能识别主要购买阻力
11. `final_decision.decision_status == selected` 时，`next_actions` 包含 `add_to_cart`

**P1 测试**：
1. "我想买个手机，平时拍照多"（目标导向）不走完整 `shopping_strategy`
2. 商品库弱支撑时，文案包含"当前商品库里更接近"降级表达
3. 使用场景（通勤/旅行）和风险敏感（敏感肌）场景正确触发

### 12.2 前端测试

**P0 必测**：
1. `criteria_card.shopping_strategy` 能解析成 Timeline 节点
2. 选购思路区出现在商品卡之前（仅 `shopping_strategy != null` 的 turn）
3. 长文案不会撑爆布局
4. 无 `shopping_strategy` 时普通推荐不受影响
5. Android 不新增 `AgentEventType.ScenarioAdvice`
6. 选购思路区能渲染 `decision_barrier.label`
7. final_decision 的 `add_to_cart` next action 复用现有加购逻辑

**P1 测试**：
1. reduce motion 下区块直接显示
2. 场景化推荐时筛选卡不抢在选购思路前面
3. 点击"换个思路"不显示用户气泡，不新增 assistant turn
4. 替换流到达首个有效 `replace_target` 前，旧内容保持可见
5. 替换失败时旧内容保持不变，只在选购思路区显示轻提示
6. 点击"补充偏好"聚焦输入框，placeholder 变为"说说你想换成什么方向"

### 12.3 真实联调用例

**P0 验收**（送礼 + 兴趣导向）：
1. "我男朋友过生日，他平时喜欢电子产品，应该送什么好？" → 送礼场景
2. "我男朋友喜欢足球，生日送什么？" → 兴趣导向场景
3. "送妈妈一款护肤品，不知道怎么选。" → 送礼 + 风险规避

**P1 验收**（扩展场景）：
4. "最近通勤想买个数码小东西，有什么建议？" → 使用场景
5. "我想买个手机，平时拍照多。" → 目标导向（预期不走完整 shopping_strategy）

期望：先给场景判断 → 再给选购方向 → 再展示商品 → 不出现字段化回复。

---

## 13. 风险与边界

| 风险 | 应对 |
|------|------|
| 场景建议变成空泛鸡汤 | 每个建议必须能转成 search_strategy |
| LLM 编造商品库没有的方向 | 加可行性检查 |
| 主时间线变复杂 | 只展示主方向，更多信息进入详情 |
| 商品出现太晚导致用户等太久 | 选购思路控制在 1 张轻卡，随后立即商品 |
| 和 criteria_card 重复 | 选购思路讲判断，criteria_card 讲筛选条件 |
| 小数据看起来推荐单薄 | 明确"当前商品库里更接近这个方向" |
| 破坏 SSE 契约 | P0 只扩展字段，不新增 event type |
| 过度追求加购导致体验变差 | 只有 `decision_status == selected` 时突出加购，置信度不足时优先对比/证据 |
| 转化话术变成强推 | 保留 risk_notes 和 not_for，强调"更稳"而不是"必须买" |

---

## 14. 推荐 Demo

### Demo A：电子产品生日礼物

> 我男朋友过生日，他平时喜欢电子产品，应该送什么好？

展示：
1. 场景判断：核心数码设备容易踩型号偏好
2. 选购思路：低踩雷黑科技小件
3. 商品候选：耳机、智能穿戴或当前商品库内最接近的数码小件
4. 推荐解释：为什么它比手机/电脑更适合作为礼物

### Demo B：足球生日礼物

> 我想送男朋友生日礼物，他喜欢足球，推荐下送什么礼物好。

展示：
1. 场景判断：不知道球队偏好时，强球队周边有风险
2. 选购思路：低风险运动休闲礼物
3. 商品候选：当前商品库里的运动休闲单品
4. 推荐解释：为什么日常能穿、运动感明显，比随机足球装备更稳

### Demo C：送妈妈护肤品

> 送妈妈的一款护肤品，不知道怎么选。

展示：
1. 场景判断：送护肤品最怕肤质和刺激风险
2. 澄清或默认方向：温和保湿、低刺激
3. 商品候选：敏感肌适用或温和型商品
4. 推荐解释：为什么先选低风险方向

---

## 15. 一句话总结

场景化推荐不能只是"提取关键词后筛商品"。它应该先帮用户做购买判断：这个场景真正难在哪里，什么方向更稳，哪些选择容易踩雷。商品卡应该作为购买策略的落地结果出现，而不是一上来就把搜索结果推到用户面前。
