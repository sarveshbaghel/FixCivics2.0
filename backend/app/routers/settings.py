"""
CivicFix - Settings Router
Admin-only endpoints to manage application settings (auto-post toggle, etc.)
"""
import logging
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from app.config import settings
from app.models import User
from app.middleware.auth import require_user

logger = logging.getLogger("civicfix.settings")
router = APIRouter(prefix="/api/v1", tags=["Settings"])

# In-memory runtime override (persists while server is running)
_runtime_settings = {
    "x_auto_post_enabled": None,  # None means "use config default"
}


class SettingsResponse(BaseModel):
    x_auto_post_enabled: bool
    x_api_connected: bool
    x_bearer_configured: bool


class SettingsUpdate(BaseModel):
    x_auto_post_enabled: bool


def is_auto_post_enabled() -> bool:
    """Check if auto-post to X is currently enabled (runtime override > config)."""
    override = _runtime_settings["x_auto_post_enabled"]
    if override is not None:
        return override
    return settings.X_AUTO_POST_ENABLED


@router.get("/settings", response_model=SettingsResponse)
async def get_settings(user: User = Depends(require_user)):
    """Get current application settings. Admin-only."""
    has_oauth_keys = all([
        settings.X_API_KEY,
        settings.X_API_SECRET,
        settings.X_ACCESS_TOKEN,
        settings.X_ACCESS_TOKEN_SECRET,
    ])

    return SettingsResponse(
        x_auto_post_enabled=is_auto_post_enabled(),
        x_api_connected=has_oauth_keys,
        x_bearer_configured=bool(settings.X_BEARER_TOKEN),
    )


@router.put("/settings", response_model=SettingsResponse)
async def update_settings(
    data: SettingsUpdate,
    user: User = Depends(require_user),
):
    """Update application settings. Admin-only."""
    _runtime_settings["x_auto_post_enabled"] = data.x_auto_post_enabled
    logger.info(
        f"Settings updated by {user.email}: "
        f"x_auto_post_enabled={data.x_auto_post_enabled}"
    )

    has_oauth_keys = all([
        settings.X_API_KEY,
        settings.X_API_SECRET,
        settings.X_ACCESS_TOKEN,
        settings.X_ACCESS_TOKEN_SECRET,
    ])

    return SettingsResponse(
        x_auto_post_enabled=is_auto_post_enabled(),
        x_api_connected=has_oauth_keys,
        x_bearer_configured=bool(settings.X_BEARER_TOKEN),
    )
