# Codex Review Action

仓库已配置 `.github/workflows/codex-review.yml`，用于自动运行 Codex Review：

- PR 创建、更新、重新打开或从 draft 转为 ready 时，审查 PR diff，并把结果评论到 PR。
- 直接 push 到 `main` 时，审查本次 push 的 commit diff，并把结果评论到最新 commit。

## GitHub Secrets

在 GitHub 仓库中进入：

`Settings` -> `Secrets and variables` -> `Actions` -> `New repository secret`

当前 workflow 需要添加：

| Secret | 用途 |
| --- | --- |
| `OPENAI_API_KEY` | 第三方中转站或 OpenAI 的 API Key |

当前 workflow 已显式使用中转站 Responses API 地址：

```text
https://www.codexauv.com/v1/responses
```

注意：`openai/codex-action` 调用的是 Responses API，不是 Chat Completions。如果第三方中转站只支持 `/v1/chat/completions`，这个 workflow 会失败，需要换成支持 `/v1/responses` 的中转端点。

## 权限

如果 workflow 没有权限评论 PR，到 GitHub 仓库：

`Settings` -> `Actions` -> `General` -> `Workflow permissions`

选择 `Read and write permissions`，并允许 GitHub Actions 创建和批准 pull request（如页面中有该选项）。
