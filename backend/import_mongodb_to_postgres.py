"""
Import data from MongoDB to PostgreSQL
Run this script to migrate your existing MongoDB reports to Render PostgreSQL
Usage: python import_mongodb_to_postgres.py
"""
import asyncio
import os
from datetime import datetime, timezone
from pymongo import MongoClient
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from app.models import User, Report
from app.database import Base
from app.config import settings
import sys

# MongoDB connection - get from environment variable
MONGO_URI = os.getenv("MONGO_URI")
if not MONGO_URI:
    print("❌ Error: MONGO_URI environment variable not set")
    print("Set it with: export MONGO_URI='your_connection_string'")
    sys.exit(1)

mongo_client = MongoClient(MONGO_URI)
mongo_db = mongo_client.get_default_database()

# PostgreSQL connection
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=False,
    future=True,
)

async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


async def import_data():
    """Import reports from MongoDB to PostgreSQL"""
    
    # Initialize PostgreSQL
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    async with async_session() as db:
        try:
            # Get reports from MongoDB
            reports_collection = mongo_db.get_collection("reports")
            mongo_reports = list(reports_collection.find({}))
            
            if not mongo_reports:
                print("❌ No reports found in MongoDB")
                return
            
            print(f"📊 Found {len(mongo_reports)} reports in MongoDB")
            
            # Import each report
            imported = 0
            for mongo_report in mongo_reports:
                try:
                    report = Report(
                        id=str(mongo_report.get("_id")),
                        issue_type=mongo_report.get("issue_type", "Other"),
                        description=mongo_report.get("description", ""),
                        latitude=mongo_report.get("latitude", 0),
                        longitude=mongo_report.get("longitude", 0),
                        address=mongo_report.get("address"),
                        incident_date=mongo_report.get("incident_date"),
                        image_url=mongo_report.get("image_url"),
                        thumbnail_url=mongo_report.get("thumbnail_url"),
                        status=mongo_report.get("status", "pending"),
                        complaint_text=mongo_report.get("complaint_text"),
                        admin_note=mongo_report.get("admin_note"),
                    )
                    db.add(report)
                    imported += 1
                except Exception as e:
                    print(f"⚠️  Error importing report: {e}")
                    continue
            
            await db.commit()
            print(f"✅ Successfully imported {imported} reports to PostgreSQL")
            
        except Exception as e:
            print(f"❌ Error importing data: {e}")
            await db.rollback()


if __name__ == "__main__":
    print("🚀 Starting MongoDB to PostgreSQL import...")
    asyncio.run(import_data())
    print("✨ Import complete!")
