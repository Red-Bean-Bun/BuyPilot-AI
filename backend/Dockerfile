FROM python:3.12-slim

WORKDIR /app

COPY pyproject.toml uv.lock ./
COPY src/ src/
COPY prompts/ prompts/
COPY static/ static/
# 使用 BuildKit 缓存挂载加速依赖安装，同时保持镜像纯净
RUN --mount=type=cache,target=/root/.cache/uv \
    pip install --no-cache-dir uv && \
    uv sync --no-dev --locked

ENV PATH="/app/.venv/bin:$PATH"

CMD ["uvicorn", "src.api.app:app", "--host", "0.0.0.0", "--port", "8000"]
