# BuyPilot-AI Demo Readiness

> 最后核实：2026-05-23
> 真实 smoke 报告：`backend/reports/demo-smoke-20260523-160717.json`

## 当前结论

后端 Demo 主链路已可演示。2026-05-23 使用真实 Postgres/pgvector 与真实模型服务跑通 `src.scripts.demo_smoke`，6/6 场景通过：

| # | 场景 | 结果 | 关键信号 |
|---|------|------|----------|
| 1 | 文字推荐：油皮洗面奶，200 元以内 | ✅ 通过 | 5 个 `product_card`，有 `criteria_card`、`final_decision`、真实 chunk evidence |
| 2 | 图片理解：官方护肤品图 + 敏感肌提问 | ✅ 通过 | `/uploads/demo_p_beauty_012_live.jpg` 进入 VL，返回 5 个商品 |
| 3 | 多轮约束：不要含酒精的防晒霜 | ✅ 通过 | 生成无酒精防晒 criteria，返回商品与最终决策 |
| 4 | 多轮改预算：预算降到 200 | ✅ 通过 | 延续上一轮防晒/无酒精约束，并更新预算 |
| 5 | 对话式加购：把这个加到购物车 | ✅ 通过 | `cart_action:add` 成功 |
| 6 | 查看购物车 | ✅ 通过 | `cart_action:view` 成功，最终 `total_items=1` |

## 复跑命令

从 `backend/` 目录执行：

```bash
DATABASE_URL=postgresql+psycopg://buypilot:buypilot@localhost:5432/buypilot ./.venv/bin/python -m src.scripts.demo_smoke
```

只做快速检查且不写报告：

```bash
DATABASE_URL=postgresql+psycopg://buypilot:buypilot@localhost:5432/buypilot ./.venv/bin/python -m src.scripts.demo_smoke --no-write
```

## 前置条件

- `deploy-postgres-1` 已启动并健康，数据库使用 `pgvector/pgvector:pg16`。
- Postgres 内已完成 reindex：100 products / 1292 chunks / 1292 embedded / 1024 dimensions。
- 项目根目录 `.env` 已配置真实 LLM、VL、Embedding、Rerank 所需 API Key。
- 官方数据目录存在：`data/raw/ecommerce_agent_dataset`。
- 演示时显式使用 Postgres `DATABASE_URL`。本地默认 `.env` 仍可能指向 SQLite，不能代表 pgvector 演示环境。

## 相关文件

- Smoke 脚本：`backend/src/scripts/demo_smoke.py`
- 报告目录：`backend/reports/`（已加入 `.gitignore`）
- Demo 图片运行时副本：`backend/uploads/demo_p_beauty_012_live.jpg`（已加入 `.gitignore`）
