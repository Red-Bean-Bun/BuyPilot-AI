# cart_items

## 服务对象

`cart_items` 支撑对话式加购和查看购物车，是 P1 轻量业务闭环能力。用户说“把这个加到购物车”时，后端会取上一轮推荐商品或意图中的目标商品写入该表。

## 为什么这样设计

- 只做购物车，不做支付、订单、库存和商家后台，符合项目裁剪范围。
- 用 `session_id` 绑定临时购物车，不引入登录用户体系。
- 同一 `session_id + product_id` 再次加购时服务层递增 `quantity`，不重复创建多行。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 购物车行 ID。 | 独立标识一条购物车记录。 |
| `session_id` | `VARCHAR`, not null | 会话 ID。 | 用户未登录时的购物车归属。 |
| `product_id` | `VARCHAR`, not null, FK | 加购商品 ID。 | 保证购物车只引用真实商品。 |
| `quantity` | `INTEGER`, not null | 数量。 | 支撑重复加购和总价计算。 |
| `added_at` | `DATETIME`, not null | 首次加购或记录创建时间。 | 按加入顺序展示购物车。 |

## 关系和索引

- 外键：`product_id -> products.id`。
- 索引：`ix_cart_items_session_id`。

## Review 关注点

- 当前没有 SKU 级选择，`product_id` 表示商品级加购。后续要做真实下单时需要增加 `sku_id`、价格快照和库存校验。
- 没有唯一约束 `session_id + product_id`，去重和递增在 `repos/cart_items.py` 中完成；并发写入扩大后应补唯一索引。
