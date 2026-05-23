# Repository Layer

Repo 层负责数据持久化（SQLModel + pgvector），包含 products、conversations、feedbacks 等表的读写操作。

边界约束：

- Repo 可以依赖 `config`、`types`、同层 Repo 模块和数据库库。
- Repo 不依赖 `services`、`runtime` 或 `api`。
- 业务编排、LLM 调用、embedding、chunking 等逻辑放在 `services/`。
