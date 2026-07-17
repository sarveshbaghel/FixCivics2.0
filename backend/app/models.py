"""
CivicFix Database Models (MongoDB)
Helper functions for creating documents with proper defaults.
MongoDB uses dictionaries rather than ORM classes.
"""
import uuid
from datetime import datetime, timezone


def utcnow():
    return datetime.now(timezone.utc)


def generate_uuid():
    return str(uuid.uuid4())


def new_user(email: str, password_hash: str = None, display_name: str = None,
             provider: str = "local", role: str = "user", user_id: str = None) -> dict:
    """Create a new user document."""
    return {
        "_id": user_id or generate_uuid(),
        "email": email,
        "password_hash": password_hash,
        "display_name": display_name,
        "provider": provider,
        "role": role,
        "created_at": utcnow(),
    }


def new_report(user_id: str = None, issue_type: str = "", description: str = "",
               latitude: float = 0.0, longitude: float = 0.0, address: str = None,
               image_url: str = None, thumbnail_url: str = None,
               complaint_text: str = None, device_id: str = None) -> dict:
    """Create a new report document."""
    now = utcnow()
    return {
        "_id": generate_uuid(),
        "user_id": user_id,
        "issue_type": issue_type,
        "description": description,
        "latitude": latitude,
        "longitude": longitude,
        "address": address,
        "image_url": image_url,
        "thumbnail_url": thumbnail_url,
        "status": "pending",
        "complaint_text": complaint_text,
        "admin_note": None,
        "posted_to_x": False,
        "x_post_id": None,
        "device_id": device_id,
        "created_at": now,
        "updated_at": now,
    }


def new_audit_log(action: str, actor: str = "system",
                  report_id: str = None, note: str = None) -> dict:
    """Create a new audit log document."""
    return {
        "_id": generate_uuid(),
        "action": action,
        "actor": actor,
        "report_id": report_id,
        "note": note,
        "timestamp": utcnow(),
    }


def doc_to_response(doc: dict) -> dict:
    """Convert a MongoDB document to an API response (rename _id to id)."""
    if doc and "_id" in doc:
        doc["id"] = doc.pop("_id")
    return doc
