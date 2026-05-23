# Service Layer

Service 层承载业务逻辑（意图识别、购买标准生成、推荐解释等）。

边界约束：

- API 和 Runtime 通过 Service 调用业务能力，不直接访问 Repo。
- Service 可以组合 Repo、LLM、embedding、reranker、prompt、chunking 等能力。
- 数据库读写细节留在 `repos/`，SSE 编排留在 `runtime/`。
