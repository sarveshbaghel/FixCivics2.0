# CivicFix — Civic Issue Reporting Platform

> **Report • Track • Resolve** — A platform for citizens to report public infrastructure problems with photo evidence, GPS location, and automatic complaint generation.

## 🏗️ Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Android App    │────▶│  FastAPI Backend  │◀────│  Admin Dashboard│
│  (Kotlin/       │     │  (Python 3.11)   │     │  (React SPA)    │
│  Jetpack        │     │                  │     │                 │
│  Compose)       │     │  POST /report    │     │  Reports List   │
│                 │     │  GET  /reports   │     │  Report Detail  │
│  📸 Camera      │     │  POST /post-to-x │     │  Map View       │
│  📍 GPS         │     │                  │     │  Admin Actions  │
│  📋 Issue Type  │     │  ┌────┐ ┌─────┐  │     │                 │
└─────────────────┘     │  │ DB │ │Redis│  │     └─────────────────┘
                        │  └────┘ └─────┘  │
                        │  ┌─────┐ ┌────┐  │
                        │  │MinIO│ │ X  │  │
                        │  └─────┘ └────┘  │
                        └──────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- [Docker](https://docker.com) & Docker Compose
- [Android Studio](https://developer.android.com/studio) (for mobile app)
- [Node.js 18+](https://nodejs.org) (for admin dashboard dev)
- [Python 3.11+](https://python.org) (for backend dev)

### 1. Clone & Configure
```bash
git clone <your-repo-url>
cd civicfix
cp .env.example .env
```

### 2. Run with Docker Compose
```bash
docker-compose up --build
```

This starts:
| Service | URL | Description |
|---------|-----|-------------|
| Backend API | http://localhost:8000 | FastAPI + Swagger UI |
| API Docs | http://localhost:8000/docs | Interactive API docs |
| Admin Dashboard | http://localhost:5173 | React admin panel |
| MinIO Console | http://localhost:9001 | S3 storage UI |
| PostgreSQL | localhost:5432 | Database |
| Redis | localhost:6379 | Rate limiting |

### 3. Login to Admin Dashboard
- URL: http://localhost:5173
- Email: `admin@civicfix.com`
- Password: `admin123`

### 4. Run Android App
1. Open `android-app/` in Android Studio
2. Update `API_BASE_URL` in `app/build.gradle.kts` if needed (default: `http://10.0.2.2:8000` for emulator)
3. Run on emulator or device

## 📁 Repository Structure

```
civicfix/
├── android-app/          # Kotlin + Jetpack Compose mobile app
│   └── app/src/main/java/com/civicfix/app/
│       ├── MainActivity.kt
│       ├── data/api/     # Retrofit API client
│       ├── data/models/  # Data classes
│       └── ui/screens/   # Compose screens (Login, Report, History)
├── backend/              # FastAPI backend
│   ├── app/
│   │   ├── main.py       # Entry point
│   │   ├── config.py     # Environment config
│   │   ├── models.py     # SQLAlchemy models
│   │   ├── schemas.py    # Pydantic schemas
│   │   ├── routers/      # API endpoints
│   │   ├── services/     # Storage, geocoding, social, complaint
│   │   ├── middleware/   # Auth, rate limiting
│   │   └── utils/        # Audit logging
│   ├── tests/            # pytest tests
│   ├── Dockerfile
│   └── requirements.txt
├── admin-dashboard/      # React SPA
│   ├── src/
│   │   ├── App.jsx       # Routes + auth context
│   │   ├── pages/        # Login, ReportsList, ReportDetail, MapView
│   │   ├── api/          # Axios client
│   │   └── index.css     # Design system
│   ├── Dockerfile
│   └── package.json
├── docs/                 # Documentation
├── docker-compose.yml    # Full stack compose
├── .env.example          # Environment template
├── Makefile              # Dev shortcuts
└── README.md             # This file
```

## 🔧 Development

### Backend Only (Mock Mode)
```bash
cd backend
pip install -r requirements.txt
set MOCK_MODE=true
set DATABASE_URL=sqlite+aiosqlite:///./civicfix.db
python -m uvicorn app.main:app --reload --port 8000
```

### Admin Dashboard Only
```bash
cd admin-dashboard
npm install
npm run dev
```

### Run Tests
```bash
# Backend
cd backend
pip install pytest pytest-asyncio httpx anyio
python -m pytest tests/ -v

# Admin Dashboard
cd admin-dashboard
npm test
```

## 🌐 API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/signup` | — | Register user |
| `POST` | `/api/v1/auth/login` | — | Login |
| `GET` | `/api/v1/auth/me` | ✅ | Get profile |
| `POST` | `/api/v1/report` | ✅ | Submit report (multipart) |
| `GET` | `/api/v1/reports` | ✅ | List reports (paginated) |
| `GET` | `/api/v1/reports/{id}` | ✅ | Get report detail |
| `PUT` | `/api/v1/reports/{id}` | 🔒 Admin | Update status/note |
| `POST` | `/api/v1/reports/{id}/post-to-x` | 🔒 Admin | Post to X |
| `GET` | `/api/v1/health` | — | Health check |

## 🔐 Environment Variables

See [`.env.example`](.env.example) for all variables. Key ones:

| Variable | Default | Description |
|----------|---------|-------------|
| `MOCK_MODE` | `true` | Simulate external services |
| `DATABASE_URL` | SQLite | Database connection |
| `S3_ENDPOINT` | MinIO | Image storage |
| `GOOGLE_MAPS_API_KEY` | — | Geocoding (optional) |
| `X_API_KEY` | — | Twitter posting (optional) |

## ✅ Acceptance Criteria

- [x] Android user can sign in, upload image, select issue type, add description, capture location, and submit
- [x] Backend stores report with image URL and reverse-geocoded address
- [x] Complaint text is generated and returned
- [x] Report appears in admin dashboard with image and address link
- [x] Admin can mark a report resolved
- [x] Post-to-X simulates when keys are missing, posts when configured
- [x] All env variables documented in `.env.example`
- [x] Docker Compose runs full stack

## 🚀 Production Deployment

See [`docs/deploy-checklist.md`](docs/deploy-checklist.md) for cloud deployment steps.

**Switch from mock to production:**
1. Set `MOCK_MODE=false`
2. Configure PostgreSQL `DATABASE_URL`
3. Add real S3 credentials
4. Add Google Maps API key
5. Add X API keys (optional)
6. Configure HTTPS (Traefik/nginx)

## 📜 License

MIT
