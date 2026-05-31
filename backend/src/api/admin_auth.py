"""Shared admin authentication dependency for /admin/* routes.

Protects observability dashboard and eval API with a simple Bearer token.
When ADMIN_API_KEY is not configured, all admin routes return 404 to avoid
exposing their existence.
"""

from __future__ import annotations

from fastapi import Header, HTTPException, Query

from src.config.settings import get_settings


async def require_admin_key(
    authorization: str | None = Header(None),
    token: str | None = Query(None),
) -> None:
    """Validate admin API key from Authorization header or query param.

    Returns 404 when no key is configured (route appears non-existent).
    Returns 401 when key is configured but missing or wrong.
    """
    expected = get_settings().admin_api_key
    if not expected:
        raise HTTPException(status_code=404)

    # Extract token from "Bearer xxx" header, fall back to query param.
    provided = None
    if authorization and authorization.startswith("Bearer "):
        provided = authorization[7:]
    elif token:
        provided = token

    if not provided or provided != expected:
        raise HTTPException(status_code=401, detail="invalid admin key")
