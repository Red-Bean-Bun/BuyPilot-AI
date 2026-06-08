#!/usr/bin/env bash
# auto-deploy.sh — 检测 origin/main 是否有新提交，有则拉取并重建后端服务
# 由 cron 驱动，不要手动交互使用
#
# Production CD script - not used in local development or evaluation.
# Usage: automated deployment on production server via cron.
#
# 设计原则：
#   1. 只 pull 不 force — 工作区脏了就跳过，不覆盖人的改动
#   2. 只 rebuild 后端相关文件有变更时才重建 — 避免无意义的停机
#   3. 全量日志 — 成功/失败/跳过都记录，方便事后排查
#   4. 飞书通知 — 部署成功/失败时推送消息到群

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_FILE="$PROJECT_DIR/deploy/auto-deploy.log"
LOCK_FILE="/tmp/buypilot-auto-deploy.lock"
MAX_LOG_LINES=2000

# 加载 .env（飞书 webhook URL 从这里读）
if [[ -f "$PROJECT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$PROJECT_DIR/.env"
    set +a
fi
FEISHU_WEBHOOK="${FEISHU_WEBHOOK_URL:-}"

# ── 工具函数 ──────────────────────────────────────

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

die() {
    log "[FAIL] $*"
    notify_feishu "[FAIL] 自动部署失败" "$*"
    exit 1
}

trim_log() {
    if [[ -f "$LOG_FILE" ]]; then
        local lines
        lines=$(wc -l < "$LOG_FILE")
        if (( lines > MAX_LOG_LINES )); then
            tail -n "$MAX_LOG_LINES" "$LOG_FILE" > "$LOG_FILE.tmp"
            mv "$LOG_FILE.tmp" "$LOG_FILE"
        fi
    fi
}

notify_feishu() {
    local title="$1"
    local body="$2"
    if [[ -z "$FEISHU_WEBHOOK" ]]; then
        return 0
    fi
    local hostname
    hostname=$(hostname -s 2>/dev/null || echo "unknown")
    curl -sf -X POST "$FEISHU_WEBHOOK" \
        -H "Content-Type: application/json" \
        -d "{
            \"msg_type\": \"interactive\",
            \"card\": {
                \"header\": {
                    \"title\": {\"tag\": \"plain_text\", \"content\": \"BuyPilot CD\"},
                    \"template\": \"blue\"
                },
                \"elements\": [
                    {
                        \"tag\": \"div\",
                        \"text\": {
                            \"tag\": \"lark_md\",
                            \"content\": \"**${title}**\n${body}\n\n${hostname} · $(date '+%m/%d %H:%M')\"
                        }
                    }
                ]
            }
        }" > /dev/null 2>&1 || true
}

# ── 前置检查 ──────────────────────────────────────

# 防止并发执行
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    log "[SKIP] 另一个 deploy 进程正在运行，跳过"
    exit 0
fi

cd "$PROJECT_DIR" || die "无法进入项目目录: $PROJECT_DIR"

# 检查 Makefile 存在
[[ -f Makefile ]] || die "Makefile 不存在，确认项目目录是否正确？"

# ── 核心逻辑 ──────────────────────────────────────

log "── 开始检测 ──"

# 1. fetch 远程最新状态
if ! git fetch origin main --quiet 2>>"$LOG_FILE"; then
    die "git fetch 失败（网络问题？SSH key 过期？）"
fi

LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main)
BASE=$(git merge-base HEAD origin/main)

log "本地 HEAD:   ${LOCAL:0:8}"
log "远程 main:   ${REMOTE:0:8}"

# 2. 比较：是否有新提交
if [[ "$LOCAL" == "$REMOTE" ]]; then
    log "[OK] 已是最新，无需更新"
    trim_log
    exit 0
fi

# 3. 检查本地是否有未推送的提交（不应自动覆盖）
if [[ "$LOCAL" != "$BASE" ]]; then
    log "[WARN] 本地有未推送的提交，跳过自动部署（请手动 push 后再试）"
    exit 0
fi

# 4. 检查工作区是否干净（允许 untracked 文件）
DIRTY_FILES=$(git status --porcelain | grep -v '^??' || true)
if [[ -n "$DIRTY_FILES" ]]; then
    log "[WARN] 工作区有未提交的修改，跳过自动部署："
    log "$DIRTY_FILES"
    exit 0
fi

# 5. 拉取最新代码
log "[PULL] 拉取新提交..."
if ! git pull --ff-only origin main 2>>"$LOG_FILE"; then
    die "git pull --ff-only 失败（可能存在分叉？请手动检查）"
fi

NEW_HEAD=$(git rev-parse HEAD)
COMMIT_MSG=$(git log -1 --format='%s' HEAD)
log "更新到:      ${NEW_HEAD:0:8} — $COMMIT_MSG"

# 6. 检查变更范围 — 只关心是否需要 rebuild
CHANGED_FILES=$(git diff --name-only "$LOCAL" "$REMOTE")
log "变更文件:"
echo "$CHANGED_FILES" | while read -r f; do log "  $f"; done

# 判断是否需要 rebuild（后端代码、Dockerfile、数据文件有变更）
NEEDS_REBUILD=false
while IFS= read -r f; do
    case "$f" in
        backend/*|data/*|deploy/*|Makefile|.env.example)
            NEEDS_REBUILD=true
            break
            ;;
    esac
done <<< "$CHANGED_FILES"

if [[ "$NEEDS_REBUILD" == "false" ]]; then
    log "[INFO] 变更不涉及后端/数据/部署文件，跳过 rebuild"
    notify_feishu "[UPDATE] 代码已更新（无需 rebuild）" "提交: ${COMMIT_MSG}\n哈希: ${NEW_HEAD:0:8}"
    trim_log
    exit 0
fi

# 7. rebuild 并重启
log "[BUILD] 开始 rebuild 后端服务..."
if make rebuild 2>&1 | tee -a "$LOG_FILE"; then
    log "[OK] 部署完成"
else
    die "make rebuild 失败，请检查日志"
fi

# 8. 健康检查
sleep 5
if curl -sf http://localhost:8000/health > /dev/null 2>&1; then
    log "[OK] 健康检查通过"
    notify_feishu "[OK] 部署成功" "提交: ${COMMIT_MSG}\n哈希: ${NEW_HEAD:0:8}\n变更: $(echo "$CHANGED_FILES" | wc -l) 个文件"
else
    log "[WARN] 健康检查未通过（服务可能仍在启动中）"
    notify_feishu "[WARN] 部署完成但健康检查未通过" "提交: ${COMMIT_MSG}\n哈希: ${NEW_HEAD:0:8}\n请检查服务状态"
fi

trim_log
log "── 检测结束 ──"
echo "" >> "$LOG_FILE"
