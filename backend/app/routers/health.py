"""
CivicFix - Health Check Router
"""
from fastapi import APIRouter
from app.config import settings
from app.schemas import HealthResponse

router = APIRouter(tags=["Health"])


@router.get("/api/v1/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(
        status="ok",
        version="1.0.0",
        mock_mode=settings.MOCK_MODE,
        database="connected",
    )
