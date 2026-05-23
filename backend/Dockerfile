FROM python:3.12-slim

WORKDIR /app

COPY pyproject.toml uv.lock ./
COPY src/ src/
RUN pip install --no-cache-dir uv \
    && uv sync --no-dev --locked

ENV PATH="/app/.venv/bin:$PATH"

CMD ["uvicorn", "src.api.app:app", "--host", "0.0.0.0", "--port", "8000"]
