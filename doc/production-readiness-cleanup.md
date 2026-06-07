# BuyPilot-AI 生产部署清理计划

> 创建日期：2026-06-07  
> 目标：将开发态代码库整理为评委可直接验收的生产部署形态  
> 状态：**待决策**  
> 最后更新：2026-06-07（追加 seed 统一方案 + data/processed 清理）

---

## 一、核心判断

**值得做**。项目功能完整性已达标（P0/P1 完成，4 条 Demo 路径可跑通），但代码库外表仍是开发中间态。评委验收窗口有限，信息噪音会直接拉低印象分。

**验收路径**：
```
clone → cp .env.example .env → 填 API Key → make rebuild → make smoke → 打开 App
```

这条链路必须零摩擦。`make rebuild` 之后应该一切就绪，不应再有额外手动步骤。

---

## 二、清理清单（按优先级分层）

### 第一层：必做（直接影响验收）

#### 1. 添加 `.dockerignore`
**问题**：当前项目根目录无 `.dockerignore`，构建 Docker 镜像时会将 `.env`（含真实 API Key）、`backend/uploads/`（开发态上传文件）、`.codegraph/`（26MB 索引）、`__pycache__/` 等全部打包进 Docker 上下文。

**建议动作**：
- 创建 `/mnt/disk1/LZJ/project/BuyPilot-AI/.dockerignore`
- 排除项：
  - `.env`
  - `.git/`
  - `backend/uploads/*`（保留目录结构，排除文件）
  - `.codegraph/`
  - `**/__pycache__/`
  - `**/*.pyc`
  - `**/*.pyo`
  - `.venv/`
  - `venv/`
  - `*.db`
  - `*.log`
  - `backend/tests/`（测试不进镜像）
  - `data/eval/`（评测样本不进运行时镜像）
  - `android/`（Android 代码不进后端镜像）
  - `deploy/`（compose 文件不进镜像）
  - `doc/`（文档不进镜像）

**影响范围**：新建文件，不影响现有代码。  
**验证方式**：`docker compose build` 后 `docker exec api ls -la /app` 确认无敏感文件。

---

#### 2. 清理 `backend/uploads/`
**问题**：当前 `backend/uploads/` 有 9 个文件（1.8MB），包含开发调试时上传的临时图片。Demo 2（拍照找货）需要保留 `demo_p_beauty_012_live.jpg`。

**建议动作**：
- 保留：`demo_p_beauty_012_live.jpg`
- 删除：其余 8 个 `upload_*.jpg` 文件
- 更新 `.gitignore`：确认 `backend/uploads/*.jpg` 已排除（当前是 `backend/uploads/` 整目录排除，OK）

**影响范围**：删除 8 个文件（约 1.6MB）。  
**验证方式**：`ls backend/uploads/` 只剩 demo 图片。

---

#### 3. 检查 `.env.example` 与 compose 环境变量一致性
**问题**：根目录 `.env.example` 中 `AUTO_SEED_ON_STARTUP=0`，但 `deploy/docker-compose.yml` 中硬编码 `AUTO_SEED_ON_STARTUP: "1"`。评委如果直接用 `.env.example` 启动（不走 compose），seed 不会自动执行，会卡住。

**建议动作**：
- 在 `.env.example` 中增加注释：
  ```bash
  # AUTO_SEED_ON_STARTUP=1 会在 API 启动时 seed DB（docker-compose 中已强制为 1）
  # 本地开发可设为 0 加速启动
  AUTO_SEED_ON_STARTUP=0
  ```
- 在 README 中强调：**必须使用 `make rebuild` 启动**，不要直接 `uvicorn`。

**影响范围**：文档修改。  
**验证方式**：评委照 README 操作能跑通。

---

### 第二层：应该做（降低信息噪音）

#### 4. 清理开发日志和临时文件
**问题**：
- `deploy/auto-deploy.log`（96KB）：CD 自动部署日志
- `.codegraph/daemon.log`：codegraph 插件日志
- `backend/src/scripts/__pycache__/`：Python 字节码缓存

这些文件不应出现在评委视野中。

**建议动作**：
- 删除：`deploy/auto-deploy.log`
- 删除：`.codegraph/daemon.log`（或整个 `.codegraph/` 目录，如果评委不需要）
- 确认 `.gitignore` 已排除 `*.log` 和 `**/__pycache__/`
- 如果已误提交，执行 `git rm --cached`

**影响范围**：删除 2 个文件。  
**验证方式**：`find . -name "*.log" -o -name "__pycache__"` 无结果。

---

#### 5. 开发脚本加注释说明
**问题**：`scripts/` 目录下有 4 个脚本，评委可能疑惑用途：
- `chat_stream_demo.py`：CLI 测试工具
- `stress_test.py`：压测脚本
- `test_compare_digital.py`：对比测试
- `auto-deploy.sh`：生产 CD 脚本（cron 驱动）

**建议动作**：
- 在每个脚本文件头部注释中增加：
  ```bash
  # ⚠️ 开发工具，评委无需关注
  # 用途：[一句话说明]
  # 使用场景：[本地开发 / 生产部署]
  ```
- 或在 `scripts/README.md` 中统一说明

**影响范围**：修改 4 个文件头部注释。  
**验证方式**：脚本功能不受影响。

---

#### 6. `backend/src/scripts/audit_domain_terms.py` 归类
**问题**：449 行的审计脚本是开发工具，不是运行时组件，放在 `src/scripts/` 会让评委误以为是核心功能。

**建议动作**：
- 移动到 `scripts/audit_domain_terms.py`
- 或保留原位，但在文件头部加注释：
  ```python
  """
  开发工具：领域术语审计
  用途：检查 domain_terms.py 中的品牌别名和品类关键词
  评委无需关注，不影响运行时功能
  """
  ```

**影响范围**：移动文件或删除文件头注释。  
**验证方式**：`grep -r "audit_domain_terms" backend/src/` 确认无其他代码引用。

---

### 第三层：可选（锦上添花）

#### 7. `data/processed/` 保留 git 追踪（已决策）
**决策**：保留 git 追踪。  
**原因**：评委 clone 后不需要额外步骤，`make rebuild` 直接可用。  
**备注**：在 README 中注明这是派生数据，由 `data/scripts/process_data.py` 从 `data/raw/` 生成。

---

#### 8. Android `local.properties` 验证
**问题**：`android/local.properties` 包含本地 SDK 路径（`sdk.dir=/mnt/disk1/LZJ/Android/Sdk`），不应被 git 追踪。

**建议动作**：
- 确认 `.gitignore` 已包含 `local.properties`（当前已包含，OK）
- 如果已误提交，执行 `git rm --cached android/local.properties`

**验证方式**：`git ls-files | grep local.properties` 无结果。

---

#### 9. README "评委验收路径" 补充预期输出
**问题**：当前 README 只给了命令，没告诉评委应该看到什么。

**建议动作**：
- 在 README "评委验收路径" 章节补充：
  ```markdown
  #### 预期输出
  
  `make db-stats` 应输出：
  ```
  products: 100
  chunks: 1292
  image_embeddings: 100
  ```
  
  `make smoke` 应输出：
  ```
  ✅ All 6 demo scenarios passed
  ```
  
  如果数字不对，运行 `make reset` 全量重置。
  ```

**影响范围**：README 文档修改。  
**验证方式**：评委能对照输出判断启动成功。

---

#### 10. 根目录过程文档处理（待决策）
**问题**：根目录有 10+ 个开发过程文档：
- `260524-handoff.md`：交接文档
- `CHAT_STREAM_DATAFLOW_STATE_MACHINE.md`：数据流状态机
- `eval-module-handoff.md`：评测模块交接
- `总评0528.md`：评审记录
- `问题整改回应.md`：问题整改
- `TODO.md`：待办事项
- `COMPOSE-AUDIT-REPORT*.md`：代码审计报告
- `BuyPilot-AI-test-code-review.md`：测试代码评审
- `问题记录.md`：问题记录
- `协作开发指南.md`：协作指南

**待决策选项**：
- **A. 移入 `doc/dev/`**：保留文件但移到子目录，根目录只留核心文档（README, CLAUDE, PRODUCT, DESIGN, design-decisions）
- **B. 直接删除**：这些文档的使命已完成，评委不需要看
- **C. 保持不动**：它们不影响验收路径

**你的决策**：______

---

### 第四层：架构优化（seed 统一方案）

#### 11. 统一 seed 流程：消除 `make seed-image` 手动步骤

**问题**：当前 text embedding 和 image embedding 的 seed 机制不一致：

| | Text Embedding | Image Embedding |
|---|---|---|
| 触发方式 | 自动（`AUTO_SEED_ON_STARTUP=1`） | 手动（`make seed-image`） |
| 幂等机制 | **计数检查**（DB 有 products+chunks 就跳过） | **内容哈希**（SHA-256 比对图片字节） |
| 幂等质量 | **弱** — 数据变了但 count>0 就不重 seed | **强** — 图片内容不变就跳过，变了就重算 |

**风险**：
- 评委验收路径断裂：`make rebuild` 之后还要跑 `make seed-image`，如果忘了，Demo 2（拍照找货）挂掉
- text seed 的幂等是定时炸弹：`source_hash` 和 `dataset_hash` 已经写进 DB，但**从未被读取来做 seed 决策**，只是装饰品

**建议动作**：

**A. 统一为自动 seed**

修改 `backend/src/services/startup.py` 的 `initialize_database()`：
```python
async def initialize_database(auto_seed: bool, strict_embeddings: bool):
    await create_db_and_tables()
    if auto_seed:
        await seed_products_if_needed(...)
        await seed_image_embeddings_if_needed()  # 新增
```

**B. 抽出 image seed 核心逻辑**

从 `backend/src/scripts/reindex_image_embeddings.py` 抽出 `reindex_image_embeddings()` 函数到 `backend/src/services/product_ingest.py` 或新建 `backend/src/services/image_seed.py`，让它可被 startup 调用。

**C. 升级 text seed 幂等机制**

修改 `backend/src/services/product_ingest.py` 的 `seed_products_if_needed()`：

```python
# 现在（count-based，弱）：
if product_count > 0 and chunk_count > 0:
    return {"seeded": False, "reason": "DB already populated"}

# 应该（hash-based，强）：
current_hash = compute_dataset_hash()  # 已有函数 _dataset_hash()
stored_hash = await get_stored_dataset_hash()  # 从 SystemMetadata 表读取
if stored_hash == current_hash:
    return {"seeded": False, "reason": "Dataset unchanged"}
```

**D. 保留 `make seed-image` 作为手动触发入口**

评委验收不需要它，但开发者调试时可能想手动重跑。保留 Makefile 目标，但内部改为调用统一后的 `seed_image_embeddings_if_needed()`。

**影响范围**：
- 修改：`backend/src/services/startup.py`（新增 image seed 调用）
- 修改：`backend/src/services/product_ingest.py`（升级 text seed 幂等）
- 重构：`backend/src/scripts/reindex_image_embeddings.py`（抽出核心逻辑）
- 修改：`backend/src/config/settings.py`（可选：新增 `AUTO_SEED_IMAGES_ON_STARTUP`，默认跟随 text seed）

**验证方式**：
1. `make reset` 全量重置后，`make rebuild` 自动完成 text + image seed
2. `make db-stats` 输出 `products: 100, chunks: 1292, image_embeddings: 100`
3. 再次 `make rebuild`（不 reset），启动日志显示 "Skipping seed: dataset unchanged" 和 "Skipping image seed: all images up-to-date"
4. 修改 `data/raw/` 中某个商品的图片，重启后 image seed 重新计算该图片的 embedding

**预期收益**：
- 评委验收路径简化为：`make rebuild → make smoke`，零额外步骤
- text seed 幂等升级后，数据变更能自动触发重 seed，避免脏数据
- 两种 seed 机制统一，代码更清晰

**风险评估**：
- **中风险**：修改 startup 流程，可能影响现有启动逻辑
- **缓解**：保留手动触发入口，失败时可回退到旧方案

---

#### 12. 清理 `data/processed/` 遗留产物

**问题**：`data/processed/products.json` 和 `data/processed/chunks.json`（共 860KB）被 git 追踪，但**运行时根本不用**。后端启动时只读 `data/raw/ecommerce_agent_dataset/`，这两个文件是早期开发遗留的离线预处理产物。

**发现**：通过代码搜索确认，`backend/src/` 中无任何代码读取 `data/processed/` 路径。

**建议动作**：

**选项 A：删除 + gitignore（推荐）**
- 删除：`data/processed/products.json` 和 `data/processed/chunks.json`
- 添加到 `.gitignore`：
  ```
  data/processed/*.json
  ```
- 如果已提交，执行 `git rm --cached data/processed/products.json data/processed/chunks.json`
- 在 `data/scripts/README.md` 中说明：`process_data.py` 是遗留工具，运行时不需要

**选项 B：保留追踪 + 加注释**
- 保留文件，但在 `data/processed/README.md` 中说明：
  ```markdown
  # data/processed/
  
  这些文件是 `data/scripts/process_data.py` 的派生产物，**运行时不使用**。
  
  后端启动时从 `data/raw/ecommerce_agent_dataset/` 直接读取原始数据。
  
  保留这些文件仅供开发调试和参考。
  ```

**影响范围**：
- 选项 A：删除 2 个文件（860KB），修改 `.gitignore`
- 选项 B：新增 1 个 README，不删文件

**验证方式**：
- 选项 A：`git ls-files data/processed/` 无结果
- 选项 B：`cat data/processed/README.md` 有说明

**风险评估**：
- 选项 A：低风险。确认无代码依赖后删除，评委不会注意到少了两个 JSON
- 选项 B：无风险。只是加文档，不改功能

---

## 三、执行顺序建议

如果全部接受，建议按以下顺序执行：

1. **第一层**（必做）：
   - 1.1 添加 `.dockerignore`
   - 1.2 清理 `backend/uploads/`
   - 1.3 检查 `.env.example` 注释
   - 预计耗时：5 分钟

2. **第二层**（应该做）：
   - 2.1 清理日志文件
   - 2.2 开发脚本加注释
   - 2.3 `audit_domain_terms.py` 归类
   - 预计耗时：10 分钟

3. **第三层**（可选）：
   - 3.1 README 补充预期输出
   - 3.2 根目录文档处理（待决策）
   - 预计耗时：5 分钟

4. **第四层**（架构优化）：
   - 4.1 统一 seed 流程（11 项）：**建议优先做**，直接影响验收路径
   - 4.2 清理 `data/processed/`（12 项）
   - 预计耗时：30-45 分钟（seed 统一涉及代码重构）

**总计**：约 50-60 分钟（不含决策时间和 seed 重构的测试时间）。

**建议优先级调整**：
- 第 11 项（seed 统一）应从第四层提升到**第一层**。理由：它直接决定评委验收路径是否零摩擦。当前评委需要 `make rebuild → make seed-image → make smoke`，目标是 `make rebuild → make smoke`。
- 第 12 项（data/processed 清理）保持第三层或第四层，不影响验收。

---

## 四、验证清单

执行完成后，逐项验证：

**基础清理验证**：
- [ ] `docker compose build` 成功，镜像内无 `.env`、`uploads/*.jpg`（除 demo）、`__pycache__`
- [ ] `ls backend/uploads/` 只剩 `demo_p_beauty_012_live.jpg`
- [ ] `find . -name "*.log" -o -name "__pycache__"` 无结果
- [ ] 根目录无开发日志、临时文件、敏感信息

**验收路径验证**：
- [ ] 评委照 README 操作能跑通完整链路
- [ ] `make rebuild` 后自动完成 text + image seed（无需 `make seed-image`）
- [ ] `make db-stats` 输出 100/1292/100
- [ ] `make smoke` 输出 6/6 passed

**幂等性验证**：
- [ ] 再次 `make rebuild`，启动日志显示 "Skipping seed: dataset unchanged"
- [ ] 修改 `data/raw/` 中某个商品图片后重启，image seed 重新计算该图片
- [ ] `data/processed/` 已清理或加说明文档

---

## 五、风险评估

| 动作 | 风险 | 缓解措施 |
|------|------|---------|
| 添加 `.dockerignore` | 无 | 新建文件，不影响现有代码 |
| 删除 `uploads/*.jpg` | 低 | 保留 demo 图片，其余是开发临时文件 |
| 移动 `audit_domain_terms.py` | 低 | 确认无其他代码引用 |
| 移动根目录文档 | 中 | 可能破坏其他文档中的相对链接 |
| 删除根目录文档 | 高 | 不可逆，建议先备份或移入子目录 |
| 统一 seed 流程 | 中 | 修改 startup 逻辑，需充分测试；保留手动触发入口可回退 |
| 升级 text seed 幂等 | 中 | hash 比对逻辑变更，需验证现有 hash 写入和读取一致性 |
| 删除 `data/processed/` | 低 | 已确认无代码依赖；保留 git 历史 |

---

## 六、待确认决策

请逐项确认或修改建议：

1. **第一层（必做）**：全部接受？还是有调整？
   - [ ] 1.1 添加 `.dockerignore`
   - [ ] 1.2 清理 `backend/uploads/`
   - [ ] 1.3 检查 `.env.example` 注释

2. **第二层（应该做）**：全部接受？还是有调整？
   - [ ] 2.1 清理日志文件
   - [ ] 2.2 开发脚本加注释
   - [ ] 2.3 `audit_domain_terms.py` 归类

3. **第三层（可选）**：接受哪些？
   - [ ] 3.1 README 补充预期输出
   - [ ] 3.2 根目录文档处理：选择 A / B / C

4. **第四层（架构优化）**：接受哪些？
   - [ ] 4.1 统一 seed 流程（**建议提升为第一层必做**）
     - 将 image seed 纳入 startup 自动流程
     - 升级 text seed 幂等为 hash-based
     - 保留 `make seed-image` 作为手动入口
   - [ ] 4.2 清理 `data/processed/`：选择 A（删除+gitignore）/ B（保留+加说明）

5. **其他**：是否有遗漏的清理项？

---

## 七、执行优先级建议

根据对验收路径的影响程度，建议实际执行顺序：

**P0（验收前必做）**：
- 第 1 项：`.dockerignore`（安全）
- 第 2 项：清理 uploads（整洁）
- 第 11 项：统一 seed 流程（**零摩擦验收路径**）

**P1（提升印象分）**：
- 第 3 项：`.env.example` 注释
- 第 4 项：清理日志
- 第 9 项：README 补充预期输出

**P2（锦上添花）**：
- 第 5 项：脚本加注释
- 第 6 项：audit_domain_terms 归类
- 第 12 项：清理 data/processed

**P3（按需处理）**：
- 第 10 项：根目录文档处理
