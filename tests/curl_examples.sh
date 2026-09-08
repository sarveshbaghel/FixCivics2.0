#!/bin/bash
# CivicFix API - curl examples
# Make sure the backend is running at http://localhost:8000

BASE_URL="http://localhost:8000"

echo "=== Health Check ==="
curl -s "$BASE_URL/api/v1/health" | python -m json.tool
echo

echo "=== Signup ==="
SIGNUP=$(curl -s -X POST "$BASE_URL/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"demo123","display_name":"Demo User"}')
echo $SIGNUP | python -m json.tool
TOKEN=$(echo $SIGNUP | python -c "import sys,json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)
echo

echo "=== Login (admin) ==="
LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@civicfix.com","password":"admin123"}')
echo $LOGIN | python -m json.tool
ADMIN_TOKEN=$(echo $LOGIN | python -c "import sys,json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)
echo

echo "=== Submit Report ==="
# Create a small test image
echo -e "\xFF\xD8\xFF\xE0" > /tmp/test.jpg
REPORT=$(curl -s -X POST "$BASE_URL/api/v1/report" \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@/tmp/test.jpg;type=image/jpeg" \
  -F "issue_type=Pothole" \
  -F "description=Large pothole on Main Street causing safety hazard" \
  -F "latitude=40.7128" \
  -F "longitude=-74.0060")
echo $REPORT | python -m json.tool
REPORT_ID=$(echo $REPORT | python -c "import sys,json; print(json.load(sys.stdin)['report_id'])" 2>/dev/null)
echo

echo "=== List Reports (admin) ==="
curl -s "$BASE_URL/api/v1/reports?page=1&page_size=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python -m json.tool
echo

echo "=== Get Report Detail ==="
curl -s "$BASE_URL/api/v1/reports/$REPORT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python -m json.tool
echo

echo "=== Update Report Status ==="
curl -s -X PUT "$BASE_URL/api/v1/reports/$REPORT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"resolved","admin_note":"Fixed by city maintenance crew"}' | python -m json.tool
echo

echo "=== Post to X (simulated) ==="
curl -s -X POST "$BASE_URL/api/v1/reports/$REPORT_ID/post-to-x" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python -m json.tool
echo

echo "=== Done! ==="
