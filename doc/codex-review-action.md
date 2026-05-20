# Codex PR Review Action

仓库已配置 `.github/workflows/codex-review.yml`，用于在 PR 创建、更新、重新打开或从 draft 转为 ready 时自动运行 Codex Review，并把结果评论到 PR。

## GitHub Secrets

在 GitHub 仓库中进入：

`Settings` -> `Secrets and variables` -> `Actions` -> `New repository secret`

需要添加：

| Secret | 用途 |
| --- | --- |
| `OPENAI_API_KEY` | 第三方中转站或 OpenAI 的 API Key |
| `CODEX_RESPONSES_API_ENDPOINT` | 第三方中转站的完整 Responses API 地址 |

`CODEX_RESPONSES_API_ENDPOINT` 必须是完整的 Responses API URL，例如：

```text
https://your-gateway.example.com/v1/responses
```

注意：`openai/codex-action` 调用的是 Responses API，不是 Chat Completions。如果第三方中转站只支持 `/v1/chat/completions`，这个 workflow 会失败，需要换成支持 `/v1/responses` 的中转端点。

## 权限

如果 workflow 没有权限评论 PR，到 GitHub 仓库：

`Settings` -> `Actions` -> `General` -> `Workflow permissions`

选择 `Read and write permissions`，并允许 GitHub Actions 创建和批准 pull request（如页面中有该选项）。
