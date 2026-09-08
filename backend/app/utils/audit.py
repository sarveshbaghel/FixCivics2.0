"""
CivicFix - Audit Log Utility
"""
import logging
from sqlalchemy.ext.asyncio import AsyncSession
from app.models import AuditLog

logger = logging.getLogger("civicfix.audit")


async def log_action(
    db: AsyncSession,
    action: str,
    actor: str | None = None,
    report_id: str | None = None,
    note: str | None = None,
):
    """Create an audit log entry."""
    entry = AuditLog(
        action=action,
        actor=actor or "system",
        report_id=report_id,
        note=note,
    )
    db.add(entry)
    await db.flush()
    logger.info(f"[AUDIT] {action} by {actor or 'system'} | report={report_id} | {note or ''}")
