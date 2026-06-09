# BuyPilot 服务器部署 SOP

本文说明如何把 BuyPilot 后端部署到云服务器，并通过 Cloudflare Tunnel 暴露 HTTPS API 给 Android APK 使用。

当前仓库的部署方案是：

- `deploy/docker-compose.yml`：PostgreSQL + pgvector + FastAPI API
- `deploy/docker-compose.cloudflare.yml`：Cloudflare Tunnel
- `Makefile`：运维命令封装
- `scripts/auto-deploy.sh`：可选 cron 自动部署

## 1. 部署目标

部署完成后应满足：

- `https://<your-api-domain>/health` 可访问
- API 容器启动后自动导入 100 条商品数据
- Postgres 内有 `products=100`、`chunks=1292`、`image_embeddings=100`
- `smoke_live_rag` 和 `demo_smoke` 可以在服务器容器内通过
- Android APK 内置 `BUY_PILOT_BASE_URL=https://<your-api-domain>`

推荐生产访问路径：

```text
Android APK -> Cloudflare HTTPS 域名 -> cloudflared 容器 -> api:8000 -> postgres:5432
```

## 2. 服务器前置条件

建议服务器配置：

- Ubuntu 22.04/24.04 或其他常见 Linux 发行版
- 2 核 4 GB 内存以上
- 20 GB 以上磁盘
- 已安装 Docker Engine 和 Docker Compose v2
- 已配置 Git 拉取仓库权限

检查命令：

```bash
docker --version
docker compose version
git --version
```

如果服务器未安装 Docker，可按云厂商或 Docker 官方说明安装。部署前确保当前用户可以执行 `docker`，否则使用 `sudo` 或把用户加入 `docker` 组。

## 3. Cloudflare Tunnel 准备

在 Cloudflare Zero Trust 中创建一个 named tunnel：

1. 进入 Zero Trust 控制台。
2. 创建 Tunnel，选择 Cloudflared。
3. 添加 Public Hostname，例如：
   ```text
   api.example.com
   ```
4. Service 类型选择 HTTP，服务地址填写：
   ```text
   http://api:8000
   ```
5. 复制 tunnel token，后面写入服务器 `.env` 的 `CLOUDFLARE_TUNNEL_TOKEN`。

注意：

- 不要使用 Quick Tunnel 承载正式 Demo。项目核心接口依赖 SSE，正式演示用 named tunnel 更稳。
- 不要提交 tunnel token 到 Git。
- 如果开放 `/admin/*`，必须设置 `ADMIN_API_KEY`，并建议用 Cloudflare Access 做团队邮箱白名单。

## 4. 拉取代码

在服务器上选择部署目录，例如 `/opt/buypilot`：

```bash
sudo mkdir -p /opt/buypilot
sudo chown "$USER":"$USER" /opt/buypilot
cd /opt/buypilot

git clone git@github.com:Red-Bean-Bun/BuyPilot-AI.git .
git checkout main
```

如果服务器不能使用 SSH，也可以使用 HTTPS clone。

## 5. 配置 `.env`

从模板复制：

```bash
cp .env.example .env
chmod 600 .env
```

至少需要填写这些变量：

```bash
# Android APK 和文档中使用的公网 API 地址。
BUY_PILOT_BASE_URL=https://api.example.com

# PostgreSQL 默认账号可用于 Demo；如改密码，要同步 POSTGRES_PASSWORD。
POSTGRES_USER=buypilot
POSTGRES_PASSWORD=replace-with-strong-password
POSTGRES_DB=buypilot

# 百炼 / Qwen。真实 live RAG、embedding、rerank 都依赖它。
BAILIAN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
BAILIAN_API_KEY=sk-your-real-bailian-key

# Doubao 是可选 fallback；没有 key 可留空。
DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
DOUBAO_API_KEY=

# Cloudflare Tunnel token。
CLOUDFLARE_TUNNEL_TOKEN=your-cloudflare-tunnel-token

# Admin API token。公网部署必须设置。
ADMIN_API_KEY=replace-with-random-token

# 可选：飞书部署通知 webhook。
FEISHU_WEBHOOK_URL=
```

生成 `ADMIN_API_KEY`：

```bash
openssl rand -hex 24
```

说明：

- `deploy/docker-compose.yml` 会覆盖容器内 `DATABASE_URL`，指向 compose 网络里的 `postgres` 服务。
- `AUTO_SEED_ON_STARTUP=1` 和 `AUTO_SEED_STRICT_EMBEDDINGS=1` 已在 compose 中强制设置。
- `BUY_PILOT_BASE_URL` 主要影响 Android APK 构建；服务器 API 自身不依赖它。

## 6. 首次启动

推荐用 Makefile：

```bash
make rebuild
```

等服务启动：

```bash
make ps
make logs
```

也可以直接运行 compose：

```bash
docker compose --env-file .env \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.cloudflare.yml \
  up --build -d
```

首次启动时，API 会自动导入商品数据并生成 embedding。这个阶段可能需要几分钟，取决于模型 API 和服务器网络。

## 7. 部署验证

### 7.1 容器状态

```bash
make ps
```

期望看到：

- `postgres` 为 healthy
- `api` 为 healthy
- `cloudflared` 为 running

### 7.2 本机健康检查

```bash
make health
```

或：

```bash
curl -sf http://localhost:8000/health | python3 -m json.tool
```

### 7.3 公网健康检查

把域名替换为你的 Cloudflare public hostname：

```bash
curl -sf https://api.example.com/health | python3 -m json.tool
```

如果本机健康检查通过但公网不通，优先检查：

- `CLOUDFLARE_TUNNEL_TOKEN` 是否正确
- Cloudflare Tunnel public hostname 的 service 是否是 `http://api:8000`
- `cloudflared` 容器日志：
  ```bash
  docker compose --env-file .env \
    -f deploy/docker-compose.yml \
    -f deploy/docker-compose.cloudflare.yml \
    logs --tail=100 cloudflared
  ```

### 7.4 数据统计

```bash
make db-stats
```

期望输出类似：

```text
products: 100
chunks: 1292
image_embeddings: 100
```

如果 `image_embeddings` 不存在或为 0，运行：

```bash
make seed-image
```

如果 text chunk embedding 缺失，运行：

```bash
make seed-text
```

### 7.5 live RAG smoke

```bash
make smoke
```

这个命令会验证真实 provider、embedding、rerank 和 `/chat/stream` 主链路。

### 7.6 Demo smoke

```bash
docker compose --env-file .env \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.cloudflare.yml \
  exec api python -m src.scripts.demo_smoke
```

这个命令覆盖 Demo 场景，适合部署后和答辩前跑一遍。

## 8. Android APK 对接服务器

在本地或构建机的项目根目录 `.env` 中设置：

```bash
BUY_PILOT_BASE_URL=https://api.example.com
ADMIN_API_KEY=your-admin-api-key
```

构建 release APK：

```bash
./scripts/build-apk.sh
```

脚本会在构建后检查 APK 中的内置后端地址。期望看到：

```text
内置后端地址: https://api.example.com
```

安装验证：

```bash
adb uninstall com.buypilot || true
adb install dist/BuyPilot-v0.1.0-release.apk
adb shell am start -n com.buypilot/.SplashActivity
```

## 9. 日常运维命令

查看服务：

```bash
make ps
```

查看 API 日志：

```bash
make logs
```

查看全部日志：

```bash
make logs-all
```

重启 API：

```bash
make restart
```

重新构建并启动：

```bash
make rebuild
```

停止服务但保留数据库：

```bash
make down
```

进入 API 容器：

```bash
make shell
```

进入 Postgres：

```bash
make db-shell
```

## 10. 自动部署

自动部署由 `scripts/auto-deploy.sh` + cron 实现。逻辑是：

1. 每 3 分钟 fetch `origin/main`
2. 如果本地落后且工作区干净，则 `git pull --ff-only`
3. 如果变更涉及 `backend/*`、`data/*`、`deploy/*`、`Makefile`、`.env.example`，执行 `make rebuild`
4. 部署结果写入 `deploy/auto-deploy.log`
5. 如果 `.env` 里配置了 `FEISHU_WEBHOOK_URL`，发送飞书通知

安装 cron：

```bash
make cd-setup
```

查看状态：

```bash
make cd-status
```

手动触发一次：

```bash
make cd-run
```

查看完整日志：

```bash
make cd-logs
```

卸载 cron：

```bash
make cd-uninstall
```

注意：

- 自动部署遇到本地已修改文件会跳过，不会覆盖人工改动。
- 自动部署只处理 `origin/main`，所以需要先把本地改动提交并 push。
- 不要在服务器上直接改代码；要改就走 Git。

## 11. 更新部署

手动更新：

```bash
git fetch origin main
git pull --ff-only origin main
make rebuild
make health
make db-stats
make smoke
```

如果只改了 Android 客户端，不涉及后端部署，服务器不一定需要 rebuild。

## 12. 回滚

先查看最近提交：

```bash
git log --oneline -10
```

临时回滚到某个提交：

```bash
git checkout <commit-sha>
make rebuild
make health
```

确认后再决定是否在本地开发机创建 revert commit 并 push 到 `main`。服务器上不建议长期停留在 detached HEAD。

## 13. 数据库重置

慎用，以下命令会删除数据库卷：

```bash
make wipe-db
```

删库并重建：

```bash
make reset
```

只有在数据结构严重不一致、旧 embedding 维度不匹配、或明确需要干净演示环境时再使用。

## 14. 安全检查

公网部署前确认：

- `.env` 没有提交到 Git
- `CLOUDFLARE_TUNNEL_TOKEN` 没有出现在日志、README、飞书文档或 commit diff 中
- `ADMIN_API_KEY` 已设置
- Cloudflare Access 至少保护 `/admin/*`
- 服务器防火墙不要对公网开放 `5432`
- 如无必要，不要直接对公网开放 `8000`，由 Cloudflare Tunnel 提供公网入口
- `OBSERVABILITY_CAPTURE_FULL_PAYLOAD` 不要在公网环境长期打开

## 15. 常见问题

### API 容器一直 unhealthy

查看日志：

```bash
make logs
```

优先检查：

- `BAILIAN_API_KEY` 是否可用
- 数据导入阶段是否在生成 embedding
- Postgres 是否 healthy
- 服务器是否能访问 DashScope / 百炼 API

### Cloudflare 域名访问不到

检查 cloudflared 日志：

```bash
docker compose --env-file .env \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.cloudflare.yml \
  logs --tail=100 cloudflared
```

确认 Cloudflare public hostname 的 service 是：

```text
http://api:8000
```

不是 `localhost:8000`。`cloudflared` 在容器内运行，应该通过 compose service name 访问 API。

### `make smoke` 失败

先确认基础数据：

```bash
make db-stats
```

再确认 provider key：

```bash
make logs
```

常见原因：

- 百炼 key 未配置或额度不足
- embedding/rerank API 网络失败
- image embeddings 未生成
- 数据库仍保留旧结构，需要重建派生表或重置演示库

### Android APK 仍然连本地地址

检查构建机 `.env`：

```bash
grep BUY_PILOT_BASE_URL .env
```

重新构建：

```bash
./scripts/build-apk.sh
```

脚本输出必须显示公网地址。

### 自动部署没有执行

查看 cron：

```bash
crontab -l
```

查看 CD 状态：

```bash
make cd-status
```

常见原因：

- 服务器工作区有未提交修改
- 本地有未 push 的提交
- `git fetch origin main` 失败
- 变更不涉及后端/数据/部署文件，因此脚本跳过 rebuild

## 16. 最短路径

首次部署最短命令清单：

```bash
git clone git@github.com:Red-Bean-Bun/BuyPilot-AI.git /opt/buypilot
cd /opt/buypilot
cp .env.example .env
chmod 600 .env

# 编辑 .env，填 BAILIAN_API_KEY、CLOUDFLARE_TUNNEL_TOKEN、ADMIN_API_KEY。
vim .env

make rebuild
make health
make db-stats
make smoke

docker compose --env-file .env \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.cloudflare.yml \
  exec api python -m src.scripts.demo_smoke
```

确认公网：

```bash
curl -sf https://api.example.com/health | python3 -m json.tool
```

可选安装自动部署：

```bash
make cd-setup
make cd-status
```
