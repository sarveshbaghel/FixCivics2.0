"""
CivicFix - Image Storage Service
Handles S3/MinIO uploads with mock mode fallback
"""
import os
import uuid
import logging
from io import BytesIO
from pathlib import Path
from PIL import Image
from app.config import settings

logger = logging.getLogger("civicfix.storage")

ALLOWED_MIME_TYPES = {"image/jpeg", "image/png"}
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"}
MAX_SIZE_BYTES = settings.MAX_UPLOAD_SIZE_MB * 1024 * 1024
THUMBNAIL_SIZE = (300, 300)


def _sanitize_filename(filename: str) -> str:
    """Generate a safe randomized filename."""
    ext = Path(filename).suffix.lower()
    if ext not in ALLOWED_EXTENSIONS:
        ext = ".jpg"
    return f"{uuid.uuid4().hex}{ext}"


def _generate_thumbnail(image_bytes: bytes) -> bytes:
    """Create a thumbnail from image bytes."""
    img = Image.open(BytesIO(image_bytes))
    img.thumbnail(THUMBNAIL_SIZE, Image.Resampling.LANCZOS)
    buffer = BytesIO()
    fmt = "PNG" if img.format == "PNG" else "JPEG"
    img.save(buffer, format=fmt, quality=85)
    return buffer.getvalue()


async def validate_image(content_type: str, size: int, filename: str) -> str | None:
    """Validate image file. Returns error message or None."""
    if content_type not in ALLOWED_MIME_TYPES:
        return f"Invalid file type '{content_type}'. Allowed: JPEG, PNG."
    if size > MAX_SIZE_BYTES:
        return f"File too large ({size / 1024 / 1024:.1f} MB). Max: {settings.MAX_UPLOAD_SIZE_MB} MB."
    ext = Path(filename).suffix.lower()
    if ext not in ALLOWED_EXTENSIONS:
        return f"Invalid file extension '{ext}'. Allowed: .jpg, .jpeg, .png."
    return None


async def upload_image(file_bytes: bytes, original_filename: str) -> dict:
    """
    Upload image and thumbnail to storage.
    Returns dict with image_url and thumbnail_url.
    """
    safe_name = _sanitize_filename(original_filename)
    thumb_name = f"thumb_{safe_name}"

    # Generate thumbnail
    try:
        thumb_bytes = _generate_thumbnail(file_bytes)
    except Exception as e:
        logger.warning(f"Thumbnail generation failed: {e}")
        thumb_bytes = None

    if settings.MOCK_MODE:
        return await _mock_upload(safe_name, thumb_name, file_bytes, thumb_bytes)
    else:
        return await _s3_upload(safe_name, thumb_name, file_bytes, thumb_bytes)


async def _mock_upload(name: str, thumb_name: str, data: bytes, thumb_data: bytes | None) -> dict:
    """Save files locally for mock mode."""
    upload_dir = Path("./mock_uploads")
    upload_dir.mkdir(exist_ok=True)

    image_path = upload_dir / name
    image_path.write_bytes(data)
    logger.info(f"[MOCK] Image saved: {image_path}")

    thumb_url = None
    if thumb_data:
        thumb_path = upload_dir / thumb_name
        thumb_path.write_bytes(thumb_data)
        thumb_url = f"/mock_uploads/{thumb_name}"
        logger.info(f"[MOCK] Thumbnail saved: {thumb_path}")

    return {
        "image_url": f"/mock_uploads/{name}",
        "thumbnail_url": thumb_url,
    }


async def _s3_upload(name: str, thumb_name: str, data: bytes, thumb_data: bytes | None) -> dict:
    """Upload files to S3/MinIO."""
    import boto3
    from botocore.config import Config

    client = boto3.client(
        "s3",
        endpoint_url=settings.S3_ENDPOINT,
        aws_access_key_id=settings.S3_ACCESS_KEY,
        aws_secret_access_key=settings.S3_SECRET_KEY,
        region_name=settings.S3_REGION,
        config=Config(signature_version="s3v4"),
    )

    # Upload main image
    content_type = "image/png" if name.endswith(".png") else "image/jpeg"
    client.put_object(
        Bucket=settings.S3_BUCKET,
        Key=f"images/{name}",
        Body=data,
        ContentType=content_type,
    )
    image_url = f"{settings.S3_ENDPOINT}/{settings.S3_BUCKET}/images/{name}"
    logger.info(f"Image uploaded to S3: {image_url}")

    # Upload thumbnail
    thumb_url = None
    if thumb_data:
        client.put_object(
            Bucket=settings.S3_BUCKET,
            Key=f"thumbnails/{thumb_name}",
            Body=thumb_data,
            ContentType=content_type,
        )
        thumb_url = f"{settings.S3_ENDPOINT}/{settings.S3_BUCKET}/thumbnails/{thumb_name}"
        logger.info(f"Thumbnail uploaded to S3: {thumb_url}")

    return {
        "image_url": image_url,
        "thumbnail_url": thumb_url,
    }
