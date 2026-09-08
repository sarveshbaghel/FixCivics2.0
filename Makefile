.PHONY: dev up down test backend-dev admin-dev

# Start all services with Docker Compose
up:
	docker-compose up --build

# Stop all services
down:
	docker-compose down

# Run backend locally (mock mode)
backend-dev:
	cd backend && pip install -r requirements.txt && MOCK_MODE=true uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Run admin dashboard locally
admin-dev:
	cd admin-dashboard && npm install && npm run dev

# Run all tests
test:
	cd backend && python -m pytest tests/ -v
	cd admin-dashboard && npm test

# Run backend tests only
test-backend:
	cd backend && python -m pytest tests/ -v

# Run database migrations
migrate:
	cd backend && alembic upgrade head
