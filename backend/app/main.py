"""
CivicFix Backend - FastAPI Application Entry Point
"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pathlib import Path
from app.config import settings
from app.database import init_db
from app.middleware.rate_limit import init_redis
from app.routers import auth, reports, health
from app.routers import settings as settings_router
from app.models import User
from app.middleware.auth import hash_password
from sqlalchemy import select, func

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(name)s | %(levelname)s | %(message)s",
)
logger = logging.getLogger("civicfix")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events."""
    logger.info("=" * 50)
    logger.info("CivicFix Backend Starting...")
    logger.info(f"  MOCK_MODE: {settings.MOCK_MODE}")
    logger.info(f"  Database: {settings.DATABASE_URL[:30]}...")
    logger.info("=" * 50)

    # Initialize database tables
    try:
        await init_db()
        # Seed admin account
        await seed_admin()
        # Seed sample reports
        await seed_reports()
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")

    # Initialize Redis for rate limiting
    try:
        await init_redis()
    except Exception as e:
        logger.error(f"Failed to initialize Redis: {e}")

    # Create mock uploads directory
    if settings.MOCK_MODE:
        Path("./mock_uploads").mkdir(exist_ok=True)

    logger.info("CivicFix Backend Ready!")
    yield
    logger.info("CivicFix Backend Shutting Down...")


async def seed_admin():
    """Create the default admin user if it doesn't exist."""
    from app.database import async_session

    async with async_session() as db:
        result = await db.execute(select(User).where(User.email == settings.ADMIN_EMAIL))
        admin = result.scalar_one_or_none()
        if not admin:
            admin = User(
                email=settings.ADMIN_EMAIL,
                password_hash=hash_password(settings.ADMIN_PASSWORD),
                display_name="Admin",
                provider="local",
                role="admin",
            )
            db.add(admin)
            await db.commit()
            logger.info(f"Admin account seeded: {settings.ADMIN_EMAIL}")
        else:
            logger.info(f"Admin account already exists: {settings.ADMIN_EMAIL}")


async def seed_reports():
    """Create sample civic issue reports if none exist."""
    from app.database import async_session
    from app.models import Report
    from datetime import datetime, timezone, timedelta

    async with async_session() as db:
        try:
            # Check report count
            from sqlalchemy import func
            result = await db.execute(select(func.count(Report.id)))
            count = result.scalar() or 0
            
            if count > 0:
                logger.info(f"Reports already exist ({count} found), skipping seed...")
                return
            
            logger.info("No reports found, starting seed...")


        sample_reports = [
            {
                "issue_type": "🕳️ Pothole",
                "description": "Large pothole on Main Street affecting traffic and causing vehicle damage",
                "latitude": 40.7128,
                "longitude": -74.0060,
                "address": "Main Street & 5th Ave, New York, NY",
                "status": "pending",
                "complaint_text": "Pothole has been there for weeks. Very dangerous."
            },
            {
                "issue_type": "🗑️ Garbage",
                "description": "Overflowing trash bins at park entrance",
                "latitude": 40.7580,
                "longitude": -73.9855,
                "address": "Central Park, New York, NY",
                "status": "approved",
                "complaint_text": "Garbage everywhere, needs immediate cleanup"
            },
            {
                "issue_type": "💡 Broken streetlight",
                "description": "Street light is non-functional making the area unsafe at night",
                "latitude": 40.7489,
                "longitude": -73.9680,
                "address": "Times Square, New York, NY",
                "status": "pending",
                "complaint_text": "Dark area at night, potential safety hazard"
            },
            {
                "issue_type": "💧 Water leakage",
                "description": "Water pipe leak causing water waste and wet sidewalk",
                "latitude": 40.7614,
                "longitude": -73.9776,
                "address": "42nd Street, New York, NY",
                "status": "resolved",
                "complaint_text": "Fixed by city maintenance",
                "admin_note": "Repair completed on 2026-08-15"
            },
            {
                "issue_type": "📋 Other",
                "description": "Damaged sidewalk creating tripping hazard",
                "latitude": 40.7505,
                "longitude": -73.9934,
                "address": "Broadway & 42nd St, New York, NY",
                "status": "rejected",
                "complaint_text": "Cracked pavement",
                "admin_note": "Already scheduled for repair by public works"
            },
            {
                "issue_type": "🕳️ Pothole",
                "description": "Multiple potholes on Park Avenue",
                "latitude": 40.7750,
                "longitude": -73.9717,
                "address": "Park Avenue, New York, NY",
                "status": "pending",
                "complaint_text": "Road conditions deteriorating rapidly"
            }
        ]

        now = datetime.now(timezone.utc)
        for i, report_data in enumerate(sample_reports):
            report = Report(
                issue_type=report_data["issue_type"],
                description=report_data["description"],
                latitude=report_data["latitude"],
                longitude=report_data["longitude"],
                address=report_data["address"],
                status=report_data["status"],
                complaint_text=report_data["complaint_text"],
                admin_note=report_data.get("admin_note"),
                incident_date=now - timedelta(days=i),
            )
            db.add(report)

        try:
            await db.commit()
            logger.info(f"Sample reports seeded: {len(sample_reports)} reports created")
        except Exception as e:
            logger.error(f"Error seeding reports: {e}")
            await db.rollback()


# Create FastAPI app
app = FastAPI(
    title="CivicFix API",
    description="Civic Issue Reporting Platform — Report public infrastructure problems in your community.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS
origins = [o.strip() for o in settings.ALLOWED_ORIGINS.split(",")]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Serve mock uploads as static files
if settings.MOCK_MODE:
    mock_dir = Path("./mock_uploads")
    mock_dir.mkdir(exist_ok=True)
    app.mount("/mock_uploads", StaticFiles(directory=str(mock_dir)), name="mock_uploads")

# Include routers
app.include_router(auth.router)
app.include_router(reports.router)
app.include_router(health.router)
app.include_router(settings_router.router)


@app.get("/", tags=["Root"])
async def root():
    return {
        "name": "CivicFix API",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/api/v1/health",
    }
