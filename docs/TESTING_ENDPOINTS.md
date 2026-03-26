# Token Optimization Testing Endpoints

## 🎯 Four Key Endpoints for Optimization Verification

### 1. Token Performance Metrics (Primary Optimization Endpoint)
**Endpoint:** `GET /api/admin/token-performance`

**Purpose:** Monitor JWT token validation cache performance and effectiveness

**Authentication:** Admin Bearer Token Required

**Request:**
```bash
curl -X GET http://localhost:8080/api/admin/token-performance \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "status": true,
  "message": "Token performance metrics retrieved",
  "data": {
    "hitCount": 12450,
    "missCount": 2150,
    "totalRequests": 14600,
    "hitRate": "85.27%",
    "evictionCount": 45,
    "loadSuccessCount": 14600,
    "loadFailureCount": 0,
    "averageLoadPenalty": "48.32 ms",
    "estimatedSize": 3421,
    "performance": "EXCELLENT",
    "recommendation": "Cache is performing optimally"
  },
  "statusCode": 200,
  "timestamp": "2026-03-25T21:30:00.000Z"
}
```

**Key Metrics to Monitor:**
- `hitRate`: Should be >80% for optimal performance
- `performance`: EXCELLENT (>80%), GOOD (60-79%), FAIR (40-59%), POOR (<40%)
- `averageLoadPenalty`: Time to validate uncached tokens (~50ms expected)
- `estimatedSize`: Current cache size (max 10,000)

**Performance Indicators:**
| Hit Rate | Status | Action |
|----------|--------|--------|
| 80-100% | ✅ EXCELLENT | None needed |
| 60-79% | ⚠️ GOOD | Monitor trends |
| 40-59% | ⚠️ FAIR | Consider increasing TTL |
| 0-39% | ❌ POOR | Increase TTL or capacity |

---

### 2. Cache Capacity Information
**Endpoint:** `GET /api/admin/token-cache-capacity`

**Purpose:** Monitor cache size, utilization, and available capacity

**Authentication:** Admin Bearer Token Required

**Request:**
```bash
curl -X GET http://localhost:8080/api/admin/token-cache-capacity \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "status": true,
  "message": "Cache capacity info retrieved",
  "data": {
    "currentSize": 3421,
    "maxSize": 10000,
    "utilizationPercent": "34.21%",
    "availableCapacity": 6579
  },
  "statusCode": 200,
  "timestamp": "2026-03-25T21:30:00.000Z"
}
```

**Key Metrics to Monitor:**
- `utilizationPercent`: Should be <75% for optimal performance
- `availableCapacity`: Remaining cache slots
- `currentSize`: Active cached tokens

**Capacity Thresholds:**
| Utilization | Status | Action |
|-------------|--------|--------|
| 0-50% | ✅ Healthy | None needed |
| 50-75% | ⚠️ Moderate | Monitor growth |
| 75-90% | ⚠️ High | Consider increasing size |
| 90-100% | ❌ Critical | Increase cache size immediately |

---

### 3. Security Audit Report (Includes Token Stats)
**Endpoint:** `GET /api/admin/security-report`

**Purpose:** Comprehensive security audit including authentication stats and token metrics

**Authentication:** Admin Bearer Token Required

**Request:**
```bash
curl -X GET http://localhost:8080/api/admin/security-report \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "status": true,
  "message": "Security report generated successfully",
  "data": {
    "authStats": {
      "successfulLogins": 1523,
      "failedLogins": 47,
      "accessDenied": 12,
      "totalAttempts": 1582,
      "successRate": 96.27
    },
    "tokenStats": {
      "activeTokens": 3421,
      "blacklistedTokens": 89,
      "totalGenerated": 3510,
      "blacklistRate": 2.54
    },
    "recentEvents": [
      {
        "timestamp": "2026-03-25T21:29:45.000Z",
        "type": "LOGIN_SUCCESS",
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "ipAddress": "192.168.1.100"
      },
      {
        "timestamp": "2026-03-25T21:29:30.000Z",
        "type": "ACCESS_DENIED",
        "userId": "550e8400-e29b-41d4-a716-446655440001",
        "ipAddress": "192.168.1.101",
        "reason": "Insufficient privileges"
      }
    ],
    "generatedAt": "2026-03-25T21:30:00.000Z"
  },
  "statusCode": 200,
  "timestamp": "2026-03-25T21:30:00.000Z"
}
```

**Key Metrics to Monitor:**
- `successRate`: Should be >90%
- `activeTokens`: Number of valid tokens in circulation
- `blacklistRate`: Should be <5%
- `recentEvents`: Last 100 security events for audit

---

### 4. User Authentication (Performance Test Endpoint)
**Endpoint:** `POST /api/auth/login`

**Purpose:** Test token generation and validation performance end-to-end

**Authentication:** None (Public endpoint)

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailAddress": "admin@example.com",
    "password": "Admin@123"
  }'
```

**Expected Response:**
```json
{
  "status": true,
  "message": "Login successful",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "role": "ADMIN",
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3ZjAwMDAwMS04YjAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJyb2xlIjoiQURNSU4iLCJ0eXBlIjoiYWNjZXNzIiwiaWF0IjoxNzExMzk1MDAwLCJleHAiOjE3MTE0ODE0MDB9.signature",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3ZjAwMDAwMS04YjAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDEiLCJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJyb2xlIjoiQURNSU4iLCJ0eXBlIjoicmVmcmVzaCIsImlhdCI6MTcxMTM5NTAwMCwiZXhwIjoxNzExOTk5ODAwfQ.signature",
    "tokenType": "Bearer",
    "expiresIn": "1 day",
    "issuedAt": "2026-03-25T21:30:00.000Z",
    "expiresAt": "2026-03-26T21:30:00.000Z",
    "refreshExpiresIn": "7 days"
  },
  "statusCode": 200,
  "timestamp": "2026-03-25T21:30:00.000Z"
}
```

**Performance Testing:**
Use this endpoint to test token generation and subsequent validation:

```bash
# 1. Login and capture token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailAddress":"admin@example.com","password":"Admin@123"}' \
  | jq -r '.data.token')

# 2. Test first request (cache miss - ~50ms)
time curl -X GET http://localhost:8080/api/admin/token-performance \
  -H "Authorization: Bearer $TOKEN"

# 3. Test second request (cache hit - <1ms)
time curl -X GET http://localhost:8080/api/admin/token-performance \
  -H "Authorization: Bearer $TOKEN"

# 4. Compare response times
# First request: ~50-60ms (JWT parsing + signature verification)
# Second request: ~1-2ms (cache hit)
# Improvement: 50x faster
```

---

## 📊 Complete Optimization Verification Workflow

### Step 1: Baseline Measurement (Before Load)
```bash
# Get initial metrics
curl -X GET http://localhost:8080/api/admin/token-performance \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Expected: Low hit count, low cache size
```

### Step 2: Generate Load
```bash
# Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailAddress":"admin@example.com","password":"Admin@123"}' \
  | jq -r '.data.token')

# Simulate 100 requests
for i in {1..100}; do
  curl -s -X GET http://localhost:8080/api/admin/security-report \
    -H "Authorization: Bearer $TOKEN" > /dev/null &
done
wait

echo "Load test complete"
```

### Step 3: Verify Cache Performance
```bash
# Check performance metrics
curl -X GET http://localhost:8080/api/admin/token-performance \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Expected results:
# - hitRate: >80%
# - performance: "EXCELLENT"
# - hitCount: ~95-99 (out of 100 requests)
# - missCount: ~1-5 (first request + occasional evictions)
```

### Step 4: Verify Cache Capacity
```bash
# Check capacity
curl -X GET http://localhost:8080/api/admin/token-cache-capacity \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Expected results:
# - utilizationPercent: <10% (for single user test)
# - availableCapacity: >9000
```

### Step 5: Security Audit
```bash
# Check security report
curl -X GET http://localhost:8080/api/admin/security-report \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# Expected results:
# - successRate: >95%
# - activeTokens: Matches cache size
# - blacklistRate: <5%
```

---

## 🧪 Performance Benchmarking Script

Save as `test-token-optimization.sh`:

```bash
#!/bin/bash

echo "=== Token Optimization Performance Test ==="
echo ""

# Configuration
BASE_URL="http://localhost:8080"
EMAIL="admin@example.com"
PASSWORD="Admin@123"

# Step 1: Login
echo "Step 1: Authenticating..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"emailAddress\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Login failed"
  exit 1
fi
echo "✅ Login successful"
echo ""

# Step 2: Clear cache (optional - for clean test)
echo "Step 2: Clearing cache for clean test..."
curl -s -X DELETE "$BASE_URL/api/admin/token-cache" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✅ Cache cleared"
echo ""

# Step 3: First request (cache miss)
echo "Step 3: Testing first request (cache miss)..."
START_TIME=$(date +%s%N)
curl -s -X GET "$BASE_URL/api/admin/token-performance" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
END_TIME=$(date +%s%N)
FIRST_REQUEST_MS=$(( ($END_TIME - $START_TIME) / 1000000 ))
echo "⏱️  First request: ${FIRST_REQUEST_MS}ms (expected: 50-60ms)"
echo ""

# Step 4: Second request (cache hit)
echo "Step 4: Testing second request (cache hit)..."
START_TIME=$(date +%s%N)
curl -s -X GET "$BASE_URL/api/admin/token-performance" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
END_TIME=$(date +%s%N)
SECOND_REQUEST_MS=$(( ($END_TIME - $START_TIME) / 1000000 ))
echo "⏱️  Second request: ${SECOND_REQUEST_MS}ms (expected: 1-5ms)"
echo ""

# Step 5: Calculate improvement
IMPROVEMENT=$(( $FIRST_REQUEST_MS / $SECOND_REQUEST_MS ))
echo "📊 Performance improvement: ${IMPROVEMENT}x faster"
echo ""

# Step 6: Load test (100 requests)
echo "Step 5: Running load test (100 requests)..."
for i in {1..100}; do
  curl -s -X GET "$BASE_URL/api/admin/security-report" \
    -H "Authorization: Bearer $TOKEN" > /dev/null &
done
wait
echo "✅ Load test complete"
echo ""

# Step 7: Get final metrics
echo "Step 6: Retrieving performance metrics..."
METRICS=$(curl -s -X GET "$BASE_URL/api/admin/token-performance" \
  -H "Authorization: Bearer $TOKEN")

HIT_RATE=$(echo $METRICS | jq -r '.data.hitRate')
PERFORMANCE=$(echo $METRICS | jq -r '.data.performance')
HIT_COUNT=$(echo $METRICS | jq -r '.data.hitCount')
MISS_COUNT=$(echo $METRICS | jq -r '.data.missCount')

echo ""
echo "=== Final Results ==="
echo "Hit Rate: $HIT_RATE"
echo "Performance: $PERFORMANCE"
echo "Cache Hits: $HIT_COUNT"
echo "Cache Misses: $MISS_COUNT"
echo ""

# Step 8: Verify success
if [ "$PERFORMANCE" == "EXCELLENT" ] || [ "$PERFORMANCE" == "GOOD" ]; then
  echo "✅ Optimization verified successfully!"
  echo "✅ Cache is performing optimally"
  exit 0
else
  echo "⚠️  Performance needs attention"
  echo "⚠️  Current status: $PERFORMANCE"
  exit 1
fi
```

**Usage:**
```bash
chmod +x test-token-optimization.sh
./test-token-optimization.sh
```

**Expected Output:**
```
=== Token Optimization Performance Test ===

Step 1: Authenticating...
✅ Login successful

Step 2: Clearing cache for clean test...
✅ Cache cleared

Step 3: Testing first request (cache miss)...
⏱️  First request: 52ms (expected: 50-60ms)

Step 4: Testing second request (cache hit)...
⏱️  Second request: 1ms (expected: 1-5ms)

📊 Performance improvement: 52x faster

Step 5: Running load test (100 requests)...
✅ Load test complete

Step 6: Retrieving performance metrics...

=== Final Results ===
Hit Rate: 98.02%
Performance: EXCELLENT
Cache Hits: 99
Cache Misses: 2

✅ Optimization verified successfully!
✅ Cache is performing optimally
```

---

## 📈 Postman Collection

Import this collection for easy testing:

```json
{
  "info": {
    "name": "Token Optimization Testing",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Login (Get Admin Token)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"emailAddress\": \"admin@example.com\",\n  \"password\": \"Admin@123\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/auth/login",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "auth", "login"]
        }
      }
    },
    {
      "name": "2. Token Performance Metrics",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/admin/token-performance",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "admin", "token-performance"]
        }
      }
    },
    {
      "name": "3. Cache Capacity Info",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/admin/token-cache-capacity",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "admin", "token-cache-capacity"]
        }
      }
    },
    {
      "name": "4. Security Report",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/admin/security-report",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "admin", "security-report"]
        }
      }
    }
  ]
}
```

---

## 🎯 Success Criteria

Your optimization is successful if:

1. ✅ **Hit Rate >80%** after 100 requests
2. ✅ **Performance = "EXCELLENT"** or "GOOD"
3. ✅ **Cache hit response time <5ms**
4. ✅ **Cache miss response time ~50ms**
5. ✅ **Utilization <75%** under normal load
6. ✅ **Success rate >90%** in security report

---

## 📝 Documentation Checklist

Use these endpoints to document:

- [ ] Baseline performance (before optimization)
- [ ] Cache hit rate after load test
- [ ] Response time comparison (miss vs hit)
- [ ] Cache capacity utilization
- [ ] Security audit results
- [ ] Performance improvement factor (X times faster)

---

## 🔗 Related Documentation

- [Token Caching Details](TOKEN_CACHING.md)
- [Performance Fixes](PERFORMANCE_FIXES.md)
- [Optimization Summary](TOKEN_OPTIMIZATION_SUMMARY.md)
- [Quick Reference](QUICK_REFERENCE.md)
