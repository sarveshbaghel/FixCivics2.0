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
from sqlalchemy import select

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
