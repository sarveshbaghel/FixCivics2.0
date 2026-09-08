"""
CivicFix Backend Tests - API endpoints
"""
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.database import init_db, engine, Base


@pytest.fixture(autouse=True)
async def setup_db():
    """Create tables before each test."""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.fixture
async def client():
    """Async test client."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as c:
        yield c


@pytest.fixture
async def admin_token(client: AsyncClient):
    """Get admin token."""
    response = await client.post("/api/v1/auth/login", json={
        "email": "admin@civicfix.com",
        "password": "admin123",
    })
    assert response.status_code == 200
    return response.json()["access_token"]


@pytest.fixture
async def user_token(client: AsyncClient):
    """Create a user and get their token."""
    response = await client.post("/api/v1/auth/signup", json={
        "email": "testuser@test.com",
        "password": "testpass123",
        "display_name": "Test User",
    })
    assert response.status_code == 201
    return response.json()["access_token"]


# --- Health Tests ---
class TestHealth:
    @pytest.mark.anyio
    async def test_health(self, client: AsyncClient):
        response = await client.get("/api/v1/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["version"] == "1.0.0"

    @pytest.mark.anyio
    async def test_root(self, client: AsyncClient):
        response = await client.get("/")
        assert response.status_code == 200
        assert "CivicFix" in response.json()["name"]


# --- Auth Tests ---
class TestAuth:
    @pytest.mark.anyio
    async def test_signup(self, client: AsyncClient):
        response = await client.post("/api/v1/auth/signup", json={
            "email": "new@test.com",
            "password": "password123",
            "display_name": "New User",
        })
        assert response.status_code == 201
        data = response.json()
        assert "access_token" in data
        assert data["role"] == "user"

    @pytest.mark.anyio
    async def test_signup_duplicate_email(self, client: AsyncClient):
        await client.post("/api/v1/auth/signup", json={
            "email": "dup@test.com", "password": "pass123"
        })
        response = await client.post("/api/v1/auth/signup", json={
            "email": "dup@test.com", "password": "pass123"
        })
        assert response.status_code == 400

    @pytest.mark.anyio
    async def test_login_success(self, client: AsyncClient, admin_token):
        response = await client.post("/api/v1/auth/login", json={
            "email": "admin@civicfix.com",
            "password": "admin123",
        })
        assert response.status_code == 200
        assert "access_token" in response.json()

    @pytest.mark.anyio
    async def test_login_wrong_password(self, client: AsyncClient):
        response = await client.post("/api/v1/auth/login", json={
            "email": "admin@civicfix.com",
            "password": "wrongpassword",
        })
        assert response.status_code == 401

    @pytest.mark.anyio
    async def test_get_me(self, client: AsyncClient, admin_token):
        response = await client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {admin_token}"}
        )
        assert response.status_code == 200
        assert response.json()["email"] == "admin@civicfix.com"


# --- Input Validation Tests ---
class TestValidation:
    @pytest.mark.anyio
    async def test_report_requires_auth_for_list(self, client: AsyncClient):
        response = await client.get("/api/v1/reports")
        assert response.status_code == 401

    @pytest.mark.anyio
    async def test_report_invalid_issue_type(self, client: AsyncClient, user_token):
        import io
        response = await client.post(
            "/api/v1/report",
            headers={"Authorization": f"Bearer {user_token}"},
            data={
                "issue_type": "InvalidType",
                "description": "Test description",
                "latitude": "40.7128",
                "longitude": "-74.0060",
            },
            files={"image": ("test.jpg", io.BytesIO(b"fake image data"), "image/jpeg")},
        )
        assert response.status_code == 400
