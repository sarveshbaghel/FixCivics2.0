"""
CivicFix - Rate Limiting Middleware
Redis-based with in-memory fallback
"""
import time
import logging
from collections import defaultdict
from typing import Optional
from fastapi import HTTPException, Request
from app.config import settings

logger = logging.getLogger("civicfix.ratelimit")

# In-memory fallback store: {key: [(timestamp), ...]}
_memory_store: dict[str, list[float]] = defaultdict(list)
_redis_client = None
_redis_available = False


async def init_redis():
    """Initialize Redis connection for rate limiting."""
    global _redis_client, _redis_available
    try:
        import redis.asyncio as aioredis
        _redis_client = aioredis.from_url(
            settings.REDIS_URL,
            decode_responses=True,
            socket_timeout=3,
            socket_connect_timeout=3,
        )
        await _redis_client.ping()
        _redis_available = True
        logger.info("Redis connected for rate limiting")
    except Exception as e:
        _redis_available = False
        logger.warning(f"Redis not available, using in-memory rate limiting: {e}")


async def check_rate_limit(identifier: str) -> None:
    """
    Check if the identifier has exceeded the rate limit.
    Raises HTTPException(429) if limit exceeded.
    """
    max_requests = settings.RATE_LIMIT_MAX_REQUESTS
    window = settings.RATE_LIMIT_WINDOW_SECONDS

    if _redis_available and _redis_client:
        await _check_redis(identifier, max_requests, window)
    else:
        _check_memory(identifier, max_requests, window)


async def _check_redis(key: str, max_req: int, window: int):
    """Redis-based sliding window rate limiter."""
    redis_key = f"ratelimit:{key}"
    now = time.time()

    pipe = _redis_client.pipeline()
    pipe.zremrangebyscore(redis_key, 0, now - window)
    pipe.zadd(redis_key, {str(now): now})
    pipe.zcard(redis_key)
    pipe.expire(redis_key, window)
    results = await pipe.execute()

    count = results[2]
    if count > max_req:
        raise HTTPException(
            status_code=429,
            detail=f"Rate limit exceeded. Max {max_req} requests per {window}s.",
        )


def _check_memory(key: str, max_req: int, window: int):
    """In-memory sliding window rate limiter (fallback)."""
    now = time.time()
    cutoff = now - window

    # Clean old entries
    _memory_store[key] = [t for t in _memory_store[key] if t > cutoff]

    if len(_memory_store[key]) >= max_req:
        raise HTTPException(
            status_code=429,
            detail=f"Rate limit exceeded. Max {max_req} requests per {window}s.",
        )

    _memory_store[key].append(now)
