"""
CivicFix - Reports Router (MongoDB)
Core API endpoints for report CRUD and social posting
"""
import logging
from typing import Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form, Query
from app.database import get_db
from app.models import new_report, doc_to_response
from app.schemas import (
    ReportResponse, ReportCreateResponse, ReportListResponse,
    PostToXResponse, AdminUpdateReport, IssueType
)
from app.middleware.auth import get_current_user, require_admin
from app.middleware.rate_limit import check_rate_limit
from app.services.storage import upload_image, validate_image
from app.services.geocoding import reverse_geocode
from app.services.social import post_to_x
from app.services.complaint import generate_complaint_text, generate_tweet_text
from app.routers.settings import is_auto_post_enabled
from app.utils.audit import log_action

logger = logging.getLogger("civicfix.reports")
router = APIRouter(prefix="/api/v1", tags=["Reports"])


@router.post("/report", response_model=ReportCreateResponse, status_code=201)
async def create_report(
    image: UploadFile = File(...),
    issue_type: str = Form(...),
    description: str = Form(...),
    latitude: float = Form(...),
    longitude: float = Form(...),
    timestamp: Optional[str] = Form(None),
    device_id: Optional[str] = Form(None),
    user: Optional[dict] = Depends(get_current_user),
):
    """Submit a new civic issue report."""
    db = get_db()

    # Rate limiting
    identifier = user["_id"] if user else (device_id or "anonymous")
    await check_rate_limit(identifier)

    # Validate issue_type
    allowed_types = [e.value for e in IssueType]
    if issue_type not in allowed_types:
        raise HTTPException(400, f"issue_type must be one of: {', '.join(allowed_types)}")

    # Validate description
    if not description or len(description) > 500:
        raise HTTPException(400, "Description is required (max 500 chars)")

    # Validate coordinates
    if not (-90 <= latitude <= 90) or not (-180 <= longitude <= 180):
        raise HTTPException(400, "Invalid coordinates")

    # Validate & upload image
    file_bytes = await image.read()
    validation_error = await validate_image(
        image.content_type or "", len(file_bytes), image.filename or "upload.jpg"
    )
    if validation_error:
        raise HTTPException(400, validation_error)

    # Upload to storage
    storage_result = await upload_image(file_bytes, image.filename or "upload.jpg")

    # Reverse geocode
    address = await reverse_geocode(latitude, longitude)

    # Generate complaint text
    complaint_text = generate_complaint_text(
        issue_type=issue_type,
        description=description,
        address=address,
        latitude=latitude,
        longitude=longitude,
    )

    # Create report document
    report = new_report(
        user_id=user["_id"] if user else None,
        issue_type=issue_type,
        description=description,
        latitude=latitude,
        longitude=longitude,
        address=address,
        image_url=storage_result["image_url"],
        thumbnail_url=storage_result.get("thumbnail_url"),
        complaint_text=complaint_text,
        device_id=device_id,
    )
    await db.reports.insert_one(report)

    # Audit log
    await log_action("report_created", actor=identifier, report_id=report["_id"])
    logger.info(f"Report created: {report['_id']} by {identifier}")

    # Auto-post to X if enabled
    if is_auto_post_enabled():
        try:
            tweet_text = generate_tweet_text(
                issue_type=issue_type,
                description=description,
                address=address or "",
                latitude=latitude,
                longitude=longitude,
            )
            x_result = await post_to_x(
                complaint_text=complaint_text,
                tweet_text=tweet_text,
                image_url=storage_result["image_url"],
            )
            update_fields = {}
            if x_result["posted_status"] == "posted":
                update_fields = {"posted_to_x": True, "x_post_id": x_result["tweet_id"]}
            elif x_result["posted_status"] == "simulated":
                update_fields = {"posted_to_x": False, "x_post_id": x_result["mock_id"]}

            if update_fields:
                await db.reports.update_one(
                    {"_id": report["_id"]}, {"$set": update_fields}
                )

            await log_action(
                "auto_posted_to_x", actor="system",
                report_id=report["_id"], note=x_result["message"]
            )
            logger.info(f"Auto-posted report {report['_id']} to X: {x_result['posted_status']}")
        except Exception as e:
            logger.warning(f"Auto-post to X failed for report {report['_id']}: {e}")

    return ReportCreateResponse(
        report_id=report["_id"],
        complaint_text=complaint_text,
        image_url=storage_result["image_url"],
        address=address,
    )


@router.get("/reports", response_model=ReportListResponse)
async def list_reports(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    issue_type: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    user: Optional[dict] = Depends(get_current_user),
):
    """List reports. Admin sees all; regular user sees own reports."""
    db = get_db()

    if not user:
        raise HTTPException(401, "Authentication required")

    # Build query filter
    query_filter = {}
    if user.get("role") != "admin":
        query_filter["user_id"] = user["_id"]

    if issue_type:
        query_filter["issue_type"] = issue_type
    if status:
        query_filter["status"] = status

    # Count total
    total = await db.reports.count_documents(query_filter)

    # Paginate
    skip = (page - 1) * page_size
    cursor = db.reports.find(query_filter).sort("created_at", -1).skip(skip).limit(page_size)
    reports_docs = await cursor.to_list(length=page_size)

    # Convert _id to id for response
    reports = []
    for doc in reports_docs:
        doc["id"] = doc.pop("_id")
        reports.append(ReportResponse(**doc))

    return ReportListResponse(
        reports=reports,
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/reports/{report_id}", response_model=ReportResponse)
async def get_report(
    report_id: str,
    user: Optional[dict] = Depends(get_current_user),
):
    """Get a single report by ID."""
    db = get_db()

    report = await db.reports.find_one({"_id": report_id})
    if not report:
        raise HTTPException(404, "Report not found")

    # Check access
    if user and user.get("role") != "admin" and report.get("user_id") != user["_id"]:
        raise HTTPException(403, "Access denied")

    report["id"] = report.pop("_id")
    return ReportResponse(**report)


@router.put("/reports/{report_id}", response_model=ReportResponse)
async def update_report(
    report_id: str,
    data: AdminUpdateReport,
    admin: dict = Depends(require_admin),
):
    """Admin: update report status or add admin note."""
    db = get_db()

    report = await db.reports.find_one({"_id": report_id})
    if not report:
        raise HTTPException(404, "Report not found")

    update_fields = {"updated_at": datetime.now(timezone.utc)}
    if data.status:
        update_fields["status"] = data.status.value
    if data.admin_note is not None:
        update_fields["admin_note"] = data.admin_note

    await db.reports.update_one({"_id": report_id}, {"$set": update_fields})
    await log_action(
        "report_updated", actor=admin["email"],
        report_id=report_id, note=f"status={data.status}, note={data.admin_note}"
    )

    # Fetch updated report
    report = await db.reports.find_one({"_id": report_id})
    report["id"] = report.pop("_id")
    return ReportResponse(**report)


@router.post("/reports/{report_id}/post-to-x", response_model=PostToXResponse)
async def post_report_to_x(
    report_id: str,
    admin: dict = Depends(require_admin),
):
    """Post a report to X (Twitter)."""
    db = get_db()

    report = await db.reports.find_one({"_id": report_id})
    if not report:
        raise HTTPException(404, "Report not found")

    if report.get("posted_to_x"):
        raise HTTPException(400, "Report already posted to X")

    tweet_text = generate_tweet_text(
        issue_type=report["issue_type"],
        description=report["description"],
        address=report.get("address", ""),
        latitude=report["latitude"],
        longitude=report["longitude"],
    )

    x_result = await post_to_x(
        complaint_text=report.get("complaint_text", ""),
        tweet_text=tweet_text,
        image_url=report.get("image_url"),
    )

    update_fields = {}
    if x_result["posted_status"] == "posted":
        update_fields = {"posted_to_x": True, "x_post_id": x_result["tweet_id"]}
    elif x_result["posted_status"] == "simulated":
        update_fields = {"posted_to_x": False, "x_post_id": x_result["mock_id"]}

    if update_fields:
        await db.reports.update_one({"_id": report_id}, {"$set": update_fields})

    await log_action(
        "posted_to_x", actor=admin["email"],
        report_id=report_id, note=x_result["message"]
    )

    return PostToXResponse(**x_result)
