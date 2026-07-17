"""
CivicFix - Authentication Middleware (MongoDB)
JWT creation/verification + Firebase ID token verification
"""
import logging
from datetime import datetime, timedelta, timezone
from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
from passlib.context import CryptContext
from app.config import settings
from app.database import get_db
import httpx

logger = logging.getLogger("civicfix.auth")

security = HTTPBearer(auto_error=False)
pwd_context = CryptContext(
    schemes=["bcrypt"], 
    deprecated="auto",
    bcrypt__truncate_error=True
)

# Cache for Google's public keys
_google_keys_cache = {"keys": None, "expires": None}
GOOGLE_CERTS_URL = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com"


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


def create_access_token(user_id: str, role: str = "user") -> str:
    """Create a JWT access token."""
    expire = datetime.now(timezone.utc) + timedelta(minutes=settings.JWT_EXPIRATION_MINUTES)
    payload = {
        "sub": user_id,
        "role": role,
        "exp": expire,
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def decode_token(token: str) -> dict:
    """Decode and validate a JWT token."""
    try:
        payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM])
        return payload
    except JWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )


async def _get_google_public_keys() -> dict:
    """Fetch Google's public keys for Firebase token verification, with caching."""
    now = datetime.now(timezone.utc)
    if _google_keys_cache["keys"] and _google_keys_cache["expires"] and _google_keys_cache["expires"] > now:
        return _google_keys_cache["keys"]

    async with httpx.AsyncClient() as client:
        response = await client.get(GOOGLE_CERTS_URL)
        response.raise_for_status()
        keys = response.json()
        _google_keys_cache["keys"] = keys
        _google_keys_cache["expires"] = now + timedelta(hours=1)
        return keys


async def verify_firebase_token(token: str) -> dict:
    """Verify a Firebase ID token."""
    from cryptography.x509 import load_pem_x509_certificate
    from cryptography.hazmat.backends import default_backend

    try:
        header = jwt.get_unverified_header(token)
        kid = header.get("kid")
        if not kid:
            raise ValueError("Token has no kid header")

        public_keys = await _get_google_public_keys()
        cert_str = public_keys.get(kid)
        if not cert_str:
            raise ValueError(f"Key ID {kid} not found in Google's public keys")

        cert = load_pem_x509_certificate(cert_str.encode(), default_backend())
        public_key = cert.public_key()

        payload = jwt.decode(
            token,
            public_key,
            algorithms=["RS256"],
            audience=settings.FIREBASE_PROJECT_ID,
            issuer=f"https://securetoken.google.com/{settings.FIREBASE_PROJECT_ID}",
        )
        return payload

    except Exception as e:
        logger.error(f"Firebase token verification failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid Firebase token: {str(e)}",
        )


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
) -> Optional[dict]:
    """Extract current user from JWT token. Returns None if no token."""
    if credentials is None:
        return None

    token = credentials.credentials
    payload = decode_token(token)
    user_id = payload.get("sub")

    if user_id is None:
        raise HTTPException(status_code=401, detail="Invalid token payload")

    db = get_db()
    user = await db.users.find_one({"_id": user_id})

    if user is None:
        raise HTTPException(status_code=401, detail="User not found")

    # Add 'id' field for convenience
    user["id"] = user["_id"]
    return user


async def require_user(
    user: Optional[dict] = Depends(get_current_user),
) -> dict:
    """Require an authenticated user."""
    if user is None:
        raise HTTPException(status_code=401, detail="Authentication required")
    return user


async def require_admin(
    user: dict = Depends(require_user),
) -> dict:
    """Require an admin user."""
    if user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
    return user
