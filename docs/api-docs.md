# CivicFix API Documentation

## Base URL
- Local: `http://localhost:8000`
- Swagger UI: `http://localhost:8000/docs`

## Authentication

All protected endpoints require a Bearer token:
```
Authorization: Bearer <access_token>
```

### POST /api/v1/auth/signup
Create a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securepass123",
  "display_name": "John Doe"
}
```

**Response (201):**
```json
{
  "access_token": "eyJhbGci...",
  "token_type": "bearer",
  "user_id": "uuid",
  "display_name": "John Doe",
  "role": "user"
}
```

### POST /api/v1/auth/login
Login with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securepass123"
}
```

---

## Reports

### POST /api/v1/report
Submit a new civic issue report. **Multipart form-data.**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| image | file | ✅ | JPEG/PNG, max 10MB |
| issue_type | string | ✅ | Pothole, Garbage, Broken streetlight, Water leakage, Other |
| description | string | ✅ | Max 500 chars |
| latitude | float | ✅ | -90 to 90 |
| longitude | float | ✅ | -180 to 180 |
| device_id | string | — | Device identifier |

**Response (201):**
```json
{
  "report_id": "uuid",
  "complaint_text": "🚨 Civic Issue Report\n...",
  "image_url": "/mock_uploads/abc123.jpg",
  "address": "123 Main St, City"
}
```

### GET /api/v1/reports
List reports (paginated). Admin sees all; users see own reports.

**Query params:** `page`, `page_size`, `issue_type`, `status`

### GET /api/v1/reports/{id}
Get a single report by ID.

### PUT /api/v1/reports/{id}
Admin: update report status or add note.

**Request:**
```json
{
  "status": "resolved",
  "admin_note": "Issue has been fixed by city crew"
}
```

### POST /api/v1/reports/{id}/post-to-x
Post report to X (Twitter). Simulates if API keys are not configured.

**Response:**
```json
{
  "posted_status": "simulated",
  "tweet_id": null,
  "mock_id": "mock_abc123",
  "message": "SIMULATED_POST — X API keys not configured."
}
```

---

## Health

### GET /api/v1/health
```json
{
  "status": "ok",
  "version": "1.0.0",
  "mock_mode": true,
  "database": "connected"
}
```
