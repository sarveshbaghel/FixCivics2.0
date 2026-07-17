"""
CivicFix Backend - FastAPI Application Entry Point (MongoDB)
"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pathlib import Path
from app.config import settings
from app.database import init_db, close_db, get_db
from app.middleware.rate_limit import init_redis
from app.models import new_user
from app.middleware.auth import hash_password
from app.routers import auth, reports, health
from app.routers import settings as settings_router

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
    logger.info(f"  Database: MongoDB Atlas")
    logger.info("=" * 50)

    # Initialize MongoDB connection
    await init_db()

    # Seed admin account
    await seed_admin()

    # Initialize Redis for rate limiting
    await init_redis()

    # Create mock uploads directory
    if settings.MOCK_MODE:
        Path("./mock_uploads").mkdir(exist_ok=True)

    logger.info("CivicFix Backend Ready!")
    yield

    # Shutdown
    await close_db()
    logger.info("CivicFix Backend Shutting Down...")


async def seed_admin():
    """Create the default admin user if it doesn't exist."""
    db = get_db()
    admin = await db.users.find_one({"email": settings.ADMIN_EMAIL})
    if not admin:
        admin_doc = new_user(
            email=settings.ADMIN_EMAIL,
            password_hash=hash_password(settings.ADMIN_PASSWORD),
            display_name="Admin",
            provider="local",
            role="admin",
        )
        await db.users.insert_one(admin_doc)
        logger.info(f"Admin account seeded: {settings.ADMIN_EMAIL}")
    else:
        logger.info(f"Admin account already exists: {settings.ADMIN_EMAIL}")


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
        "database": "MongoDB Atlas",
        "docs": "/docs",
        "health": "/api/v1/health",
    }
