"""
CivicFix - Auth Router (MongoDB)
Signup, Login, Firebase login, and user profile endpoints
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, status
from app.database import get_db
from app.models import new_user, doc_to_response
from app.schemas import UserCreate, UserLogin, TokenResponse, UserResponse, FirebaseLoginRequest
from app.middleware.auth import (
    hash_password, verify_password, create_access_token, require_user,
    verify_firebase_token
)

logger = logging.getLogger("civicfix.auth")
router = APIRouter(prefix="/api/v1/auth", tags=["Authentication"])


@router.post("/signup", response_model=TokenResponse, status_code=201)
async def signup(data: UserCreate):
    """Register a new user account (legacy - email/password)."""
    db = get_db()

    # Check if email already exists
    existing = await db.users.find_one({"email": data.email})
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")

    user = new_user(
        email=data.email,
        password_hash=hash_password(data.password),
        display_name=data.display_name,
        provider="local",
        role="user",
    )
    await db.users.insert_one(user)

    token = create_access_token(user["_id"], user["role"])
    logger.info(f"New user registered: {user['email']}")

    return TokenResponse(
        access_token=token,
        user_id=user["_id"],
        display_name=user["display_name"],
        role=user["role"],
    )


@router.post("/login", response_model=TokenResponse)
async def login(data: UserLogin):
    """Login with email and password (legacy)."""
    db = get_db()

    user = await db.users.find_one({"email": data.email})
    if not user or not user.get("password_hash"):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    if not verify_password(data.password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    token = create_access_token(user["_id"], user["role"])
    logger.info(f"User logged in: {user['email']}")

    return TokenResponse(
        access_token=token,
        user_id=user["_id"],
        display_name=user.get("display_name"),
        role=user["role"],
    )


@router.post("/firebase-login", response_model=TokenResponse)
async def firebase_login(data: FirebaseLoginRequest):
    """Login/register via Firebase ID token."""
    db = get_db()

    # Verify the Firebase ID token
    firebase_payload = await verify_firebase_token(data.firebase_token)

    firebase_uid = firebase_payload.get("user_id") or firebase_payload.get("sub")
    email = firebase_payload.get("email")
    name = firebase_payload.get("name") or firebase_payload.get("display_name", "")

    if not email:
        raise HTTPException(status_code=400, detail="Firebase token does not contain email")

    # Find or create user
    user = await db.users.find_one({"email": email})

    if user is None:
        user = new_user(
            user_id=firebase_uid,
            email=email,
            display_name=name or email.split("@")[0],
            provider="firebase",
            role="user",
            password_hash=None,
        )
        await db.users.insert_one(user)
        logger.info(f"Firebase user registered: {email} (uid: {firebase_uid})")
    else:
        if name and user.get("display_name") != name:
            await db.users.update_one(
                {"_id": user["_id"]},
                {"$set": {"display_name": name}}
            )
            user["display_name"] = name
        logger.info(f"Firebase user logged in: {email}")

    token = create_access_token(user["_id"], user["role"])

    return TokenResponse(
        access_token=token,
        user_id=user["_id"],
        display_name=user.get("display_name"),
        role=user["role"],
    )


@router.get("/me", response_model=UserResponse)
async def get_me(user: dict = Depends(require_user)):
    """Get current user profile."""
    return UserResponse(
        id=user["_id"],
        email=user["email"],
        display_name=user.get("display_name"),
        role=user["role"],
        created_at=user["created_at"],
    )
