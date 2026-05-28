# SwipeDeck 决策收敛算法与前后端调整

> 日期：2026-05-28  
> 状态：最新产品与算法协作决策  
> 关联文档：`doc/prd/05-商品优先异步渐进导购流程.md`

## 1. 决策摘要

`SwipeDeck` 是偏好信号采集层，不是用户必须完成的任务。

`final_decision` 不能只是“用户说继续后让 LLM 总结候选商品”。它应该由后端先运行显式决策算法，再让 LLM 负责解释算法结果。

核心公式：

```text
final_decision = 决策算法排序结果 + LLM 自然语言解释
```

而不是：

```text
final_decision = LLM 自由选择一个商品
```

## 2. 用户路径原则

用户不需要按固定路径滑完所有卡。

```text
用户滑了很多 → 决策更个性化
用户只点开几张 → 用浏览信号辅助决策
用户完全不滑 → 用 RAG + criteria 默认排序决策
用户中途退出 → 基于已有行为做当前最优决策
```

`final_decision` 应该随时可触发：

```text
点击“帮我选一个”
输入“继续”
输入“给最终建议”
输入“收敛建议”
加购后询问“就这个吗”
退出 Swipe 页面回到聊天流后点收敛
```

不要求：

```text
不要求滑完所有卡
不要求至少右滑一个
不要求必须打开详情
```

## 3. 候选数量分支

### 3.1 0 张商品

没有候选时，不应该输出伪商品或硬选结论。

目标流：

```text
thinking
→ text_delta(no_match)
→ criteria_card
→ done(completed 或 awaiting_criteria_adjustment)
```

行为：

```text
说明当前条件下没有合适商品
指出可能导致无结果的条件
引导用户调整筛选条件
不生成 final_decision
不生成 product_card
```

### 3.2 1 张商品

只有 1 张候选时，不进入 SwipeDeck。

原因：

```text
Swipe 的价值是从多个候选里表达偏好。
只有 1 张时，滑动没有意义，还会显得流程绕。
```

目标流：

```text
thinking
→ product_card(rank=1)
→ criteria_card
→ final_decision
→ done(completed)
```

这时 `final_decision` 的语义不是“从多张里帮你选”，而是：

```text
这唯一候选是否适合你
为什么适合
有哪些风险
如果不满意，应该怎么调筛选条件
```

前端不展示 `ProductSwipeModeScreen` 入口，或将入口弱化为“查看详情”，不要引导用户左滑 / 右滑。

### 3.3 2 张及以上商品

有 2 张及以上候选时，进入候选卡组和 SwipeDeck。

目标流：

```text
thinking
→ product_card(rank=1, deck_id=xxx)
→ product_card(rank=2, deck_id=xxx)
→ product_card(rank=3, deck_id=xxx)
→ criteria_card
→ done(awaiting_product_feedback, deck_id=xxx)
```

随后：

```text
用户 swipe / 点开详情 / 看证据 / 加购 / 直接继续
→ 后端读取已有行为信号
→ 运行决策算法
→ final_decision
→ done(completed)
```

## 4. 用户行为信号

后端需要把用户行为转成可解释偏好信号。

建议权重：

```text
add_to_cart: +1.5
like / right_swipe: +1.0
view_detail: +0.35
open_evidence: +0.25
current_card_dwell: +0.1
not_interested / left_swipe: -1.2
explicit_dislike_reason: -1.5
```

解释：

```text
打开详情不是喜欢，只是弱正向兴趣
查看证据表示用户在关注可信依据，是弱正向信号
右滑是明确正向信号
左滑是明确负向信号
加购是最强正向信号
未操作商品不扣分，继续使用 RAG 和 criteria 分数
```

## 5. 决策评分算法

建议后端为每个候选商品计算：

```text
final_score =
  retrieval_score * 0.35
+ criteria_match_score * 0.25
+ user_signal_score * 0.25
+ evidence_score * 0.10
- risk_penalty * 0.05
```

字段含义：

```text
retrieval_score：RAG 召回 / rerank 得分
criteria_match_score：与当前 criteria 的结构化匹配程度
user_signal_score：Swipe、浏览、证据、加购等用户行为信号
evidence_score：证据质量与覆盖度
risk_penalty：风险提示、硬约束冲突、用户排除项冲突
```

算法结果应产出：

```text
winner_product_id
ranked_candidates
alternatives
excluded_or_penalized_products
score_breakdown
decision_confidence
```

LLM 只基于这些结果生成解释，不负责自由挑选 winner。

## 6. 特殊行为处理

本节只讨论产品态决策不足，不覆盖以下工程异常：

```text
LLM 决策生成失败 / 超时
LLM 返回非法 winner_product_id
前后端状态丢失，例如 deck_id 过期
```

这些工程异常应另行设计 error / fallback 机制。本节只处理：

```text
A. 没有候选商品
B. 用户把所有候选都排除了
C. 候选商品有，但没有足够信号做强结论
```

### 6.1 用户完全不滑

用户直接点“帮我选一个”：

```text
user_signal_score = 0
使用 retrieval_score + criteria_match_score + evidence_score - risk_penalty
输出当前最优 final_decision
```

决策卡文案应说明：

```text
基于当前候选和筛选条件，我建议优先选...
```

### 6.2 用户只点开几张

用户打开详情 / 看证据，但没有 swipe：

```text
view_detail / open_evidence 作为弱正向信号
未浏览商品仍保留原始排序竞争资格
```

不能只在浏览过的商品里选。

### 6.3 用户只滑了几张，没滑完

已滑过商品：

```text
like / dislike 强信号生效
```

未滑过商品：

```text
user_signal_score = 0
继续使用 RAG + criteria 分数
```

最终仍在全部候选中重排，不能只从已滑商品中选。

### 6.4 用户左滑了全部商品

不要硬选 Top1。

目标输出：

```text
当前候选都不太符合你的偏好
总结被排除的原因
建议调整 criteria
可以返回新的 product deck
```

可输出：

```text
final_decision(no_suitable_winner)
```

或者用 `text_delta + criteria_card` 引导重新筛选。

### 6.5 用户右滑多个商品

右滑商品优先进入 winner / alternative 竞争。

如果多个都合适：

```text
首选 A
备选 B
A 更适合当前需求
B 更适合另一个偏好
```

## 7. 决策不足状态与下一轮设计

这三类情况不应简单当作失败。它们应该是可继续推进的“决策未完成态”：系统解释为什么当前不能给强结论，并给出下一轮可执行路径。

### 7.1 A：没有候选商品

场景：

```text
当前 criteria 下 RAG 没检索到商品。
```

本轮输出：

```text
text_delta：当前条件下暂时没有匹配商品
criteria_card：展示导致无结果的筛选条件
done(awaiting_criteria_adjustment)
```

不输出：

```text
product_card
final_decision
```

原因：没有候选时没有可决策对象，不能伪造商品，也不能硬给最终建议。

前端展示：

```text
无匹配说明卡
当前筛选卡
主 CTA：调整筛选
快捷 CTA：放宽预算 / 换个品类 / 去掉排除项 / 换一组
输入框提示：也可以直接说“预算高一点”“不要限制品牌”
```

下一轮设计：

```text
用户点 criteria_card 调整
或用户直接说“预算放宽到300”“可以含香精”“换成面霜”
→ 后端生成新 criteria
→ 重新检索
→ 返回新的 product_card deck
```

状态语义：

```text
decision_status = no_match
next_step = adjust_criteria
```

### 7.2 B：用户把所有候选都排除了

场景：

```text
当前 deck 有商品，但用户全部左滑 / not_interested。
```

本轮输出建议：

```text
final_decision(decision_status = no_suitable_winner)
→ criteria_card
→ done(awaiting_criteria_adjustment)
```

或者：

```text
text_delta：当前候选都不太符合你的偏好
→ criteria_card
→ done(awaiting_criteria_adjustment)
```

更推荐第一种，因为它仍然属于“对当前 deck 的收敛结论”，只是结论是“不应硬选”。

不应输出：

```text
winner_product_id = 某个被排除商品
```

前端展示：

```text
“当前候选不适合”决策卡
说明：你排除了这些候选，我不会强行推荐
总结可能原因：不够温和 / 价格不合适 / 品牌不喜欢 / 成分不合适
主 CTA：换一组
次 CTA：调整筛选
```

下一轮设计：

```text
用户点“换一组”
→ 后端保留原 criteria
→ 加入 avoid_products = 当前 deck 全部商品
→ 重新检索新的 product_card deck

用户点“调整筛选”
或说“那预算高一点”“不要这几个品牌”
→ 后端合并新 criteria
→ 重新检索
```

区别：

```text
换一组 = criteria 不变，只排除当前 deck
调整筛选 = criteria 改变，再检索
```

状态语义：

```text
decision_status = no_suitable_winner
next_step = replace_deck 或 adjust_criteria
```

### 7.3 C：候选商品有，但没有足够信号做强结论

场景：

```text
用户没有滑
只看了 1 张
多个候选分数很接近
没有明显 winner
```

这类情况不应该卡住。用户点“帮我选一个”时，通常期待系统给出建议。

本轮输出建议：

```text
final_decision(decision_status = selected, confidence = low)
→ done(completed)
```

只有在系统真的无法区分候选时，才使用：

```text
decision_status = needs_more_signal
```

前端展示：

```text
“我先给一个当前建议”
首选 A
说明：基于当前筛选和已有浏览/滑动信息，A 暂时最稳
提示：如果你再看 1-2 个商品，我可以更准
CTA：继续看候选 / 换一组 / 调整筛选 / 加入购物车
```

下一轮设计：

```text
用户接受
→ 加购 / 结束

用户继续看候选
→ 回到原 deck，不重新检索
→ 用户继续 swipe / view_detail
→ 再次触发 final_decision
→ 后端用新增信号重新评分

用户说“再温和一点 / 不要酒精”
→ 修改 criteria
→ 新 deck
```

这类情况优先让用户继续在当前 deck 里补信号，不默认重新检索。

状态语义：

```text
decision_status = selected
confidence = low
next_step = continue_current_deck 或 accept_recommendation
```

必要时：

```text
decision_status = needs_more_signal
next_step = continue_current_deck
```

### 7.4 状态字段建议

为支持 A/B/C，建议 `final_decision` 或决策相关 payload 增加：

```text
decision_status:
  selected
  no_match
  no_suitable_winner
  needs_more_signal

confidence:
  high
  medium
  low

next_step:
  adjust_criteria
  replace_deck
  continue_current_deck
  accept_recommendation
```

三类状态总结：

```text
A no_match:
当前条件无商品
→ 下一轮必须调整 criteria 后重新检索

B no_suitable_winner:
有商品，但用户都排除了
→ 下一轮换一组或调整 criteria

C low_confidence:
有商品，但信号不足
→ 可以给低置信度建议
→ 下一轮继续看当前 deck 或调整 criteria
```

## 8. 后端调整

### 8.1 新增 Decision Scoring 模块

建议新增：

```text
backend/src/services/decision_scoring.py
```

职责：

```text
读取当前 deck 商品
读取 criteria
读取 retrieval trace / evidence
读取 feedback
计算 score_breakdown
产出 ranked decision candidates
```

Runtime 层不直接写评分细节，只调用 service。

### 8.2 final_decision 输入改造

当前 `continue_decision_from_current_deck` 应从：

```text
products + criteria + feedback → LLM decision
```

改为：

```text
products + criteria + feedback + evidence + retrieval features
→ decision_scoring
→ ranked_candidates
→ LLM explanation
→ final_decision
```

LLM prompt 中必须明确：

```text
不得改变 winner_product_id
不得推荐 ranked_candidates 之外的商品
必须基于 score_breakdown 解释
```

### 8.3 候选数量分支

`continue_recommendation_from_criteria` 在拿到 retrieval products 后应分支：

```text
if len(products) == 0:
    emit no_match text + criteria_card + done

elif len(products) == 1:
    emit product_card + criteria_card + final_decision + done(completed)

else:
    emit product_card* + criteria_card + done(awaiting_product_feedback, deck_id)
```

### 8.4 反馈数据要求

`/feedback` 需要记录：

```text
session_id
deck_id
product_id
action
feedback_type
reason
created_at
```

如果当前接口还没有 `deck_id`，必须补上；否则无法区分不同候选组。

### 8.5 测试要求

新增或修改后端测试：

```text
0 张候选不发 product_card / selected final_decision
1 张候选直接 final_decision，不进入 awaiting_product_feedback
2 张及以上候选进入 awaiting_product_feedback
未 swipe 也能 final_decision
只浏览部分商品时仍从全部候选中决策
左滑全部商品时不硬选 winner
右滑多个商品时 winner 优先来自右滑集合
LLM 不得覆盖 scoring 给出的 winner_product_id
no_match 下一轮必须调整 criteria 后重新检索
no_suitable_winner 下一轮支持 replace_deck 和 adjust_criteria
low confidence 下一轮支持 continue_current_deck
```

## 9. 前端调整

### 9.1 0 张候选

展示：

```text
无匹配说明
criteria_card / 当前筛选卡
调整筛选入口
```

不展示 ProductRecommendationStrip。

### 9.2 1 张候选

聊天流展示单个商品推荐卡。

不要强引导进入 SwipeDeck。可提供：

```text
查看详情
加入购物车
换一组
调整筛选
```

如果后端同轮发 `final_decision`，前端直接展示 `DecisionSummaryCard`。

### 9.3 2 张及以上候选

继续使用：

```text
ProductRecommendationStrip
ProductSwipeModeScreen
```

但 CTA 要表达“可选反馈”，不是“必须滑完”：

```text
帮我选一个
先看详情
继续筛选
```

### 9.4 用户中途退出 Swipe 页面

退出时不丢失已记录的本地 swipe 状态。

如果用户回到聊天流点击“帮我选一个”：

```text
用当前 deck_id 触发 final_decision
不要求用户回去滑完
```

### 9.5 显示偏好信号状态

ProductRecommendationStrip 可展示轻状态：

```text
已看 2 个｜喜欢 1 个｜排除 1 个
```

没有任何行为时，不显示压力文案，不暗示必须操作。

### 9.6 收敛入口

候选卡组出现后始终提供：

```text
帮我选一个
```

该入口在以下情况都可点击：

```text
未滑任何商品
只浏览过商品
只滑过部分商品
滑完全部商品
```

### 9.7 决策不足态展示

前端需要按状态区分展示，不要把所有情况都渲染成普通最终推荐卡。

```text
no_match：
显示“当前条件下没有匹配商品”
主按钮：调整筛选

no_suitable_winner：
显示“当前候选都不太合适”
主按钮：换一组
次按钮：调整筛选

selected + low confidence：
显示“我先给一个当前建议”
主按钮：加入购物车 / 查看详情
次按钮：继续看候选

needs_more_signal：
显示“我还需要一点偏好信号”
主按钮：继续看候选
次按钮：仍然帮我选一个
```

## 10. 事件流示例

### 10.1 0 张商品

```text
thinking
→ text_delta("当前条件下暂时没有匹配商品...")
→ criteria_card
→ done(awaiting_criteria_adjustment)
```

### 10.2 1 张商品

```text
thinking
→ product_card(rank=1)
→ criteria_card
→ final_decision
→ done(completed)
```

### 10.3 多张商品，用户不滑直接收敛

```text
Turn 1:
thinking
→ product_card(rank=1)
→ product_card(rank=2)
→ product_card(rank=3)
→ criteria_card
→ done(awaiting_product_feedback, deck_id=deck_1)

Turn 2:
用户：“帮我选一个”
→ thinking(decision)
→ final_decision
→ done(completed)
```

### 10.4 多张商品，用户滑部分后收敛

```text
Turn 1:
product_card* → criteria_card → done(awaiting_product_feedback)

Feedback:
view_detail(p1)
like(p1)
not_interested(p2)

Turn 2:
用户：“继续”
→ decision_scoring 读取 p1/p2 行为和未操作候选
→ final_decision
→ done(completed)
```

### 10.5 用户排除全部后换一组

```text
Turn 1:
product_card* → criteria_card → done(awaiting_product_feedback)

Feedback:
not_interested(p1)
not_interested(p2)
not_interested(p3)

Turn 2:
用户：“换一组”
→ final_decision(decision_status=no_suitable_winner)
→ criteria_card
→ done(awaiting_criteria_adjustment)

Turn 3:
用户确认换一组 / 系统自动换一组
→ 后端保留 criteria，加入 avoid_products=[p1,p2,p3]
→ product_card*(new deck)
→ criteria_card
→ done(awaiting_product_feedback)
```

### 10.6 低置信度建议后继续看

```text
Turn 1:
product_card* → criteria_card → done(awaiting_product_feedback)

Turn 2:
用户：“帮我选一个”
→ final_decision(decision_status=selected, confidence=low)
→ done(completed)

Turn 3:
用户：“我再看看”
→ 前端回到原 deck
→ 用户继续 view_detail / swipe
→ 再次收敛时后端用新增信号重新评分
```

## 11. 最终口径

```text
SwipeDeck 提供偏好信号；
final_decision 根据已有信号随时收敛；
没有信号时按 RAG 和 criteria 决策；
候选只有 1 张时不进入 SwipeDeck，直接给适配判断；
A 没有候选 → 调筛选再检索；
B 全部排除 → 换一组或调筛选；
C 信号不足 → 给低置信度建议，并允许继续看当前 deck；
信号不足时决策卡明确说明“基于当前候选和你已表达的偏好”。
```

这套设计能保证用户怎么走都能闭环，不会被 Swipe 流程绑架，同时也让 Swipe 行为真正参与最终决策。
