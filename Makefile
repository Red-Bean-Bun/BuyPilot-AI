# BuyPilot-AI 运维 Makefile
# 从项目根目录执行

SHELL := /bin/bash
COMPOSE := docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.cloudflare.yml --env-file .env

.PHONY: help up down rebuild restart logs logs-all ps wipe-db reset seed-image seed-text smoke health db-stats shell db-shell test test-local lint cd-setup cd-status cd-run cd-logs

help: ## 显示帮助
	@echo "BuyPilot-AI 运维命令"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

# ── 生命周期 ──────────────────────────────────────

up: ## 启动所有服务（不重建镜像）
	$(COMPOSE) up -d

down: ## 停止所有服务（保留数据卷）
	$(COMPOSE) down

rebuild: ## 重建镜像并启动
	$(COMPOSE) up --build -d

restart: ## 重启 api 服务
	$(COMPOSE) restart api

logs: ## 查看 api 日志（tail 50）
	$(COMPOSE) logs --tail=50 api

logs-all: ## 查看所有服务日志
	$(COMPOSE) logs --tail=100

ps: ## 查看容器状态
	$(COMPOSE) ps

# ── 数据库 ────────────────────────────────────────

wipe-db: ## 删除数据库卷（⚠️ 丢失所有数据）
	$(COMPOSE) down -v
	@echo "数据库卷已删除，重新 rebuild 将自动 seed"

reset: ## 删库 + 重建镜像 + seed（全量重置）
	$(COMPOSE) down -v
	$(COMPOSE) up --build -d
	@echo "等待 api 健康..."
	@sleep 15
	$(COMPOSE) logs --tail=30 api

# ── 数据与索引 ────────────────────────────────────

seed-image: ## 构建图片 embedding 索引（需要百炼 API Key）
	$(COMPOSE) exec api python -m src.scripts.reindex_image_embeddings

seed-text: ## 重建 text chunk embedding（通常不需要，auto seed 已覆盖）
	$(COMPOSE) exec api python -m src.scripts.reindex_embeddings

# ── 验证 ──────────────────────────────────────────

smoke: ## 运行 live RAG smoke test
	$(COMPOSE) exec api python -m src.scripts.smoke_live_rag

health: ## 检查 API 健康状态
	@curl -sf http://localhost:8000/health | python3 -m json.tool || echo "❌ API 不可达"

define DB_STATS_PY
import asyncio
from sqlmodel import select, func
from src.repos.database import get_session
from src.repos.models import Product, ProductChunk, Conversation
async def s():
    async for db in get_session():
        for m,n in [(Product,'products'),(ProductChunk,'chunks'),(Conversation,'conversations')]:
            r = await db.exec(select(func.count()).select_from(m))
            print(f'  {n}: {r.one()}')
        try:
            from src.repos.models import ProductImageEmbedding
            r = await db.exec(select(func.count()).select_from(ProductImageEmbedding))
            print(f'  image_embeddings: {r.one()}')
        except Exception:
            print('  image_embeddings: table not found')
        break
asyncio.run(s())
endef
export DB_STATS_PY

db-stats: ## 查看数据库表行数统计
	$(COMPOSE) exec api python -c "$$DB_STATS_PY"

# ── 调试 ──────────────────────────────────────────

shell: ## 进入 api 容器 shell
	$(COMPOSE) exec api bash

db-shell: ## 进入 postgres psql
	$(COMPOSE) exec postgres psql -U buypilot -d buypilot

# ── 测试 ──────────────────────────────────────────

test: ## 在容器内运行测试
	$(COMPOSE) exec api python -m pytest -q

test-local: ## 在本地 venv 运行测试
	cd backend && uv run pytest -q

lint: ## 本地 ruff 检查
	cd backend && uv run ruff check src tests

# ── 自动部署（CD）───────────────────────────────────

cd-setup: ## 安装 cron 定时任务（每 3 分钟检测 origin/main 变更）
	@chmod +x scripts/auto-deploy.sh
	@(crontab -l 2>/dev/null | grep -v 'auto-deploy.sh'; echo "*/3 * * * * $(CURDIR)/scripts/auto-deploy.sh") | crontab -
	@echo "✅ cron 已安装：每 3 分钟检测一次"
	@echo "   查看: crontab -l"
	@echo "   卸载: make cd-uninstall"

cd-status: ## 查看 CD 状态（cron 任务 + 最近日志）
	@echo "── cron 任务 ──"
	@crontab -l 2>/dev/null | grep auto-deploy || echo "  未安装（运行 make cd-setup）"
	@echo ""
	@echo "── 最近 10 条日志 ──"
	@tail -10 deploy/auto-deploy.log 2>/dev/null || echo "  暂无日志"

cd-run: ## 手动触发一次检测（调试用）
	scripts/auto-deploy.sh

cd-logs: ## 查看 CD 完整日志
	tail -50 deploy/auto-deploy.log

cd-uninstall: ## 卸载 cron 定时任务
	@(crontab -l 2>/dev/null | grep -v 'auto-deploy.sh') | crontab -
	@echo "✅ cron 已卸载"
