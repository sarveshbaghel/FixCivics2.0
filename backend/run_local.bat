@echo off
echo ===================================
echo CivicFix Backend - Local Dev Mode
echo ===================================
echo MOCK_MODE is enabled
echo Database: MongoDB Atlas
set MOCK_MODE=true
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
