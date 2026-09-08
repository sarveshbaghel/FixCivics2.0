# CivicFix Deploy Checklist

## Prerequisites
- Docker + Docker Compose OR cloud provider account (Cloud Run, ECS, Heroku)
- Domain name (optional)

## 1. Prepare Environment

```bash
cp .env.example .env
```

Edit `.env` with production values:
- [ ] `MOCK_MODE=false`
- [ ] `DATABASE_URL` — PostgreSQL connection string
- [ ] `REDIS_URL` — Redis connection string
- [ ] `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET` — AWS S3 or compatible
- [ ] `GOOGLE_MAPS_API_KEY` — For reverse geocoding
- [ ] `JWT_SECRET` — Strong random secret
- [ ] `ADMIN_EMAIL`, `ADMIN_PASSWORD` — Change defaults!
- [ ] `X_API_KEY`, `X_API_SECRET`, `X_ACCESS_TOKEN`, `X_ACCESS_TOKEN_SECRET` — (optional)

## 2. Database Setup
```bash
# Run migrations
cd backend
alembic upgrade head
```

## 3. Deploy Backend

### Docker
```bash
docker build -t civicfix-backend ./backend
docker run -p 8000:8000 --env-file .env civicfix-backend
```

### Cloud Run
```bash
gcloud run deploy civicfix-backend \
  --source ./backend \
  --port 8000 \
  --set-env-vars "$(cat .env | tr '\n' ',')"
```

## 4. Deploy Admin Dashboard

```bash
cd admin-dashboard
npm run build
# Serve dist/ via nginx, Cloudflare Pages, Vercel, etc.
```

## 5. HTTPS Setup

Use Traefik, nginx, or cloud provider's load balancer for TLS termination.

## 6. Verify
- [ ] `GET /api/v1/health` returns `status: ok`
- [ ] Admin can login at dashboard URL
- [ ] Report submission works end-to-end
- [ ] Images are stored in S3
- [ ] Geocoding returns real addresses

## Security Checklist
- [ ] Change default admin password
- [ ] Use strong JWT_SECRET
- [ ] Enable HTTPS
- [ ] Restrict ALLOWED_ORIGINS to production domains
- [ ] Review rate limiting settings
