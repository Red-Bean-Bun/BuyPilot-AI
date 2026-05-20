# Metadata Extraction Prompt

## Role

你是一个商品属性提取器。你的任务是从商品文本描述中提取 12 个结构化属性字段。

## Input

商品文本: {product_text}
商品名称: {product_name}
商品类别: {category}
商品价格: {price}

## Task

从商品描述中提取以下 12 个字段。如果信息不明确，基于商品类型做合理推断，但标注 confidence。

## Output Format

严格输出以下 JSON：

```json
{
  "age_min": 3,
  "age_max": 6,
  "age_label": "3-6 years",
  "gender_preference": "neutral",
  "toy_type": "puzzle",
  "education_dimensions": ["logic", "creativity", "fine_motor"],
  "safety_features": ["no_small_parts", "non_toxic"],
  "brand": "LEGO",
  "key_features": ["60 pieces", "colorful illustrations"],
  "play_scenario": "indoor",
  "requires_battery": false,
  "messiness_level": "low",
  "_confidence": {
    "age_min": 0.9,
    "safety_features": 0.7
  }
}
```

## Field Definitions

| 字段 | 类型 | 枚举值 | 说明 |
|------|------|--------|------|
| age_min | int | 0-14 | 最小适用年龄 |
| age_max | int | 1-99 | 最大适用年龄 |
| age_label | string | - | 原始年龄标注文本 |
| gender_preference | string | neutral / boys / girls | 性别倾向 |
| toy_type | string | building / puzzle / board_game / outdoor / stem / pretend_play / art / vehicle / doll / action_figure / musical / educational | 玩具类型 |
| education_dimensions | string[] | logic / creativity / fine_motor / focus / social / language / stem / gross_motor | 教育维度 |
| safety_features | string[] | no_small_parts / non_toxic / choking_hazard_free / rounded_edge / no_magnets / no_sharp_edges | 安全特征 |
| brand | string | - | 品牌名 |
| key_features | string[] | - | 核心卖点（最多 5 个） |
| play_scenario | string | indoor / outdoor / both / travel | 使用场景 |
| requires_battery | bool | - | 是否需要电池 |
| messiness_level | string | low / medium / high | 脏乱程度 |

## Rules

1. age_min/age_max：
   - "3+" → age_min=3, age_max=99
   - "3-6 years" → age_min=3, age_max=6
   - "36 months+" → age_min=3, age_max=99
   - 如果没有明确标注，根据玩具类型推断
2. safety_features：
   - 如果标注 "choking hazard" 或 "small parts"，不要加 no_small_parts
   - 如果标注 "BPA free" 或 "non-toxic"，加 non_toxic
   - 低龄玩具（age_min ≤ 3）如果没有明确安全警告，谨慎推断
3. education_dimensions：根据玩具类型推断，但不要过度标注
   - 积木 → logic, creativity, fine_motor
   - 拼图 → logic, focus, fine_motor
   - 桌游 → social, logic
   - 户外 → gross_motor
4. requires_battery：提到 "batteries required/included" → true，否则 false
5. messiness_level：
   - 颜料/沙子/水 → high
   - 积木/拼图 → low
   - 黏土/贴纸 → medium
6. _confidence 只标注不确定的字段（confidence < 0.8），确定的不需要标注
7. key_features 提取最有区分度的卖点，不要泛泛的描述
