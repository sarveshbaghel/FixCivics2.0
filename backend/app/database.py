"""
CivicFix Database Setup
MongoDB async client using Motor
"""
import logging
from motor.motor_asyncio import AsyncIOMotorClient
from app.config import settings

logger = logging.getLogger("civicfix.database")

# MongoDB client (initialized on startup)
client: AsyncIOMotorClient = None
db = None


async def init_db():
    """Initialize MongoDB connection."""
    global client, db
    
    # Use certifi for SSL certificate verification on Windows
    import certifi
    client = AsyncIOMotorClient(
        settings.MONGODB_URL, 
        tlsCAFile=certifi.where(),
        tls=True,
        tlsAllowInvalidCertificates=True
    )
    
    db = client[settings.MONGODB_DB_NAME]

    # Create indexes for performance
    await db.users.create_index("email", unique=True)
    await db.reports.create_index("user_id")
    await db.reports.create_index("created_at")
    await db.audit_logs.create_index("report_id")

    # Ping to verify connection
    await client.admin.command("ping")
    logger.info(f"Connected to MongoDB: {settings.MONGODB_DB_NAME}")


async def close_db():
    """Close MongoDB connection."""
    global client
    if client:
        client.close()
        logger.info("MongoDB connection closed")


def get_db():
    """Get the database instance."""
    return db
