"""
CivicFix - Audit Log Utility (MongoDB)
"""
import logging
from app.database import get_db
from app.models import new_audit_log

logger = logging.getLogger("civicfix.audit")


async def log_action(
    action: str,
    actor: str | None = None,
    report_id: str | None = None,
    note: str | None = None,
):
    """Create an audit log entry in MongoDB."""
    db = get_db()
    entry = new_audit_log(
        action=action,
        actor=actor or "system",
        report_id=report_id,
        note=note,
    )
    await db.audit_logs.insert_one(entry)
    logger.info(f"[AUDIT] {action} by {actor or 'system'} | report={report_id} | {note or ''}")
