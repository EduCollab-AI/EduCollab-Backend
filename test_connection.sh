#!/bin/bash

echo "🔍 Testing Railway → Supabase Connection"
echo "========================================"
echo ""

# Test 1: Health Check
echo "1️⃣ Testing Health Endpoint..."
HEALTH_RESPONSE=$(curl -s https://educollab-backend-production.up.railway.app/health)
if [ "$HEALTH_RESPONSE" = "OK" ]; then
    echo "   ✅ Health check passed: $HEALTH_RESPONSE"
else
    echo "   ❌ Health check failed: $HEALTH_RESPONSE"
fi
echo ""

# Test 2: Registration Endpoint
echo "2️⃣ Testing Registration Endpoint..."
TIMESTAMP=$(date +%s)
REGISTER_RESPONSE=$(curl -s -X POST https://educollab-backend-production.up.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"test_${TIMESTAMP}@example.com\",
    \"password\": \"Test123456!\",
    \"firstName\": \"Test\",
    \"lastName\": \"Connection\",
    \"role\": \"parent\",
    \"phone\": \"1234567890\"
  }")

if echo "$REGISTER_RESPONSE" | grep -q '"success":true'; then
    echo "   ✅ Registration successful!"
    echo "   Response: $(echo "$REGISTER_RESPONSE" | head -c 300)"
    echo "..."
elif echo "$REGISTER_RESPONSE" | grep -q "Failed to resolve"; then
    echo "   ❌ Connection failed: Environment variables not set correctly"
    echo "   Error: $REGISTER_RESPONSE"
elif echo "$REGISTER_RESPONSE" | grep -q "400 Bad Request"; then
    echo "   ⚠️  Supabase API error: 400 Bad Request"
    echo "   Error: $REGISTER_RESPONSE"
else
    echo "   Response: $REGISTER_RESPONSE"
fi
echo ""

echo "========================================"
