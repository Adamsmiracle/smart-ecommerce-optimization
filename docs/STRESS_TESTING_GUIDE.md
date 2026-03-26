# E-Commerce API Stress Testing & Performance Report

## 🎯 Four Core Endpoints for Stress Testing

### 1. **GET /api/products** - Get All Products (Read-Heavy)
### 2. **GET /api/orders/{id}** - Get Order by ID (Cached Read)
### 3. **POST /api/orders** - Create Order (Write-Heavy + Transaction)
### 4. **GET /api/users** - Get All Users (Admin Query)

---

## 📋 Test Environment Setup

### Prerequisites
```bash
# Install Apache JMeter
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip
unzip apache-jmeter-5.6.3.zip

# Or use Apache Bench (simpler)
sudo apt-get install apache2-utils

# Or use wrk (high-performance)
sudo apt-get install wrk
```

### Test Data Setup
```bash
# 1. Login as Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailAddress":"admin@example.com","password":"Admin@123"}' \
  | jq -r '.data.token')

# 2. Login as Customer
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailAddress":"customer@example.com","password":"Customer@123"}' \
  | jq -r '.data.token')

# 3. Get a product ID for testing
PRODUCT_ID=$(curl -s -X GET "http://localhost:8080/api/products?size=1" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  | jq -r '.data.content[0].id')

# 4. Get a user ID for testing
USER_ID=$(curl -s -X GET "http://localhost:8080/api/users?size=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.data.content[0].id')

echo "ADMIN_TOKEN=$ADMIN_TOKEN"
echo "CUSTOMER_TOKEN=$CUSTOMER_TOKEN"
echo "PRODUCT_ID=$PRODUCT_ID"
echo "USER_ID=$USER_ID"
```

---

## 🧪 Endpoint 1: GET /api/products (Read-Heavy)

### Purpose
Test read performance with caching, pagination, and concurrent users

### Endpoint Details
```
GET /api/products?page=0&size=20
Authorization: Bearer {token}
```

### Test Script (Apache Bench)
```bash
# Test 1: Baseline (100 requests, 10 concurrent)
ab -n 100 -c 10 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/products?page=0&size=20

# Test 2: Moderate Load (1000 requests, 50 concurrent)
ab -n 1000 -c 50 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/products?page=0&size=20

# Test 3: Heavy Load (5000 requests, 100 concurrent)
ab -n 5000 -c 100 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/products?page=0&size=20

# Test 4: Stress Test (10000 requests, 200 concurrent)
ab -n 10000 -c 200 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/products?page=0&size=20
```

### Test Script (wrk - High Performance)
```bash
# Create wrk script
cat > products-test.lua << 'EOF'
wrk.method = "GET"
wrk.headers["Authorization"] = "Bearer " .. os.getenv("CUSTOMER_TOKEN")
wrk.headers["Content-Type"] = "application/json"
EOF

# Run test: 30 seconds, 10 threads, 100 connections
wrk -t10 -c100 -d30s \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/products?page=0&size=20
```

### Expected Results (Before Optimization)
```
Requests per second:    200-300 req/s
Time per request:       50-80ms (mean)
Time per request:       3-5ms (mean, across all concurrent)
Transfer rate:          150-200 KB/sec
```

### Expected Results (After Optimization)
```
Requests per second:    1500-2000 req/s
Time per request:       5-10ms (mean)
Time per request:       0.5-1ms (mean, across all concurrent)
Transfer rate:          1000-1500 KB/sec
Cache Hit Rate:         85-95%
```

### Metrics to Collect
```bash
# During test, monitor:
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/admin/token-performance | jq

# Expected output:
{
  "hitRate": "92.45%",
  "performance": "EXCELLENT",
  "averageLoadPenalty": "2.34 ms"
}
```

---

## 🧪 Endpoint 2: GET /api/orders/{id} (Cached Read)

### Purpose
Test single-entity retrieval with caching and token validation

### Endpoint Details
```
GET /api/orders/{orderId}
Authorization: Bearer {token}
```

### Setup Test Data
```bash
# Create a test order first
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"quantity\": 2
      }
    ],
    \"shippingAddressId\": \"550e8400-e29b-41d4-a716-446655440000\",
    \"paymentMethodId\": \"550e8400-e29b-41d4-a716-446655440001\"
  }")

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.data.id')
echo "ORDER_ID=$ORDER_ID"
```

### Test Script (Apache Bench)
```bash
# Test 1: Baseline (100 requests, 10 concurrent)
ab -n 100 -c 10 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/orders/$ORDER_ID

# Test 2: Moderate Load (1000 requests, 50 concurrent)
ab -n 1000 -c 50 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/orders/$ORDER_ID

# Test 3: Heavy Load (5000 requests, 100 concurrent)
ab -n 5000 -c 100 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/orders/$ORDER_ID
```

### Test Script (Custom Bash)
```bash
#!/bin/bash
# get-order-stress-test.sh

ORDER_ID=$1
TOKEN=$2
REQUESTS=1000
CONCURRENT=50

echo "=== Order Retrieval Stress Test ==="
echo "Order ID: $ORDER_ID"
echo "Requests: $REQUESTS"
echo "Concurrent: $CONCURRENT"
echo ""

# Warm up cache
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/orders/$ORDER_ID > /dev/null

# Run test
START_TIME=$(date +%s)

for i in $(seq 1 $REQUESTS); do
  (
    RESPONSE_TIME=$(curl -s -w "%{time_total}" -o /dev/null \
      -H "Authorization: Bearer $TOKEN" \
      http://localhost:8080/api/orders/$ORDER_ID)
    echo $RESPONSE_TIME
  ) &
  
  # Control concurrency
  if [ $(jobs -r | wc -l) -ge $CONCURRENT ]; then
    wait -n
  fi
done

wait

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "=== Results ==="
echo "Total Duration: ${DURATION}s"
echo "Throughput: $((REQUESTS / DURATION)) req/s"
```

### Expected Results
```
Before Optimization:
- First request: 50-60ms (cache miss)
- Subsequent: 45-55ms (no cache)
- Throughput: 200-300 req/s

After Optimization:
- First request: 50-60ms (cache miss)
- Subsequent: 1-2ms (cache hit)
- Throughput: 1500-2000 req/s
- Cache Hit Rate: 99%+ (same order)
```

---

## 🧪 Endpoint 3: POST /api/orders (Write-Heavy + Transaction)

### Purpose
Test write performance, transaction handling, and database load

### Endpoint Details
```
POST /api/orders
Authorization: Bearer {token}
Content-Type: application/json

Body:
{
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2
    }
  ],
  "shippingAddressId": "uuid",
  "paymentMethodId": "uuid"
}
```

### Test Script (Apache Bench with POST)
```bash
# Create request body file
cat > order-request.json << EOF
{
  "userId": "$USER_ID",
  "items": [
    {
      "productId": "$PRODUCT_ID",
      "quantity": 1
    }
  ],
  "shippingAddressId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethodId": "550e8400-e29b-41d4-a716-446655440001"
}
EOF

# Test 1: Baseline (50 requests, 5 concurrent)
ab -n 50 -c 5 -p order-request.json -T application/json \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/orders

# Test 2: Moderate Load (200 requests, 20 concurrent)
ab -n 200 -c 20 -p order-request.json -T application/json \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/orders

# Test 3: Heavy Load (500 requests, 50 concurrent)
ab -n 500 -c 50 -p order-request.json -T application/json \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  http://localhost:8080/api/orders
```

### Test Script (Custom with Cleanup)
```bash
#!/bin/bash
# create-order-stress-test.sh

TOKEN=$1
USER_ID=$2
PRODUCT_ID=$3
REQUESTS=100
CONCURRENT=10

echo "=== Order Creation Stress Test ==="
echo "Requests: $REQUESTS"
echo "Concurrent: $CONCURRENT"
echo ""

SUCCESS=0
FAILED=0
TOTAL_TIME=0

START_TIME=$(date +%s)

for i in $(seq 1 $REQUESTS); do
  (
    START=$(date +%s%N)
    
    RESPONSE=$(curl -s -w "\n%{http_code}" \
      -X POST http://localhost:8080/api/orders \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"userId\": \"$USER_ID\",
        \"items\": [{\"productId\": \"$PRODUCT_ID\", \"quantity\": 1}],
        \"shippingAddressId\": \"550e8400-e29b-41d4-a716-446655440000\",
        \"paymentMethodId\": \"550e8400-e29b-41d4-a716-446655440001\"
      }")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    END=$(date +%s%N)
    DURATION=$(( (END - START) / 1000000 ))
    
    if [ "$HTTP_CODE" = "201" ]; then
      echo "SUCCESS,$DURATION"
    else
      echo "FAILED,$HTTP_CODE,$DURATION"
    fi
  ) &
  
  if [ $(jobs -r | wc -l) -ge $CONCURRENT ]; then
    wait -n
  fi
done

wait

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "=== Results ==="
echo "Total Duration: ${DURATION}s"
echo "Throughput: $((REQUESTS / DURATION)) req/s"
```

### Expected Results
```
Before Optimization:
- Average response: 150-250ms
- Throughput: 40-60 req/s
- Success rate: 95-98%
- Database connections: High

After Optimization:
- Average response: 100-150ms
- Throughput: 60-100 req/s
- Success rate: 98-99%
- Database connections: Moderate
- Token validation: <1ms (cached)
```

---

## 🧪 Endpoint 4: GET /api/users (Admin Query)

### Purpose
Test admin-level queries with pagination and authorization

### Endpoint Details
```
GET /api/users?page=0&size=20
Authorization: Bearer {admin_token}
```

### Test Script (Apache Bench)
```bash
# Test 1: Baseline (100 requests, 10 concurrent)
ab -n 100 -c 10 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/users?page=0&size=20

# Test 2: Moderate Load (500 requests, 25 concurrent)
ab -n 500 -c 25 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/users?page=0&size=20

# Test 3: Heavy Load (2000 requests, 50 concurrent)
ab -n 2000 -c 50 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/users?page=0&size=20
```

### Test Script (wrk)
```bash
# 30 second test, 10 threads, 50 connections
wrk -t10 -c50 -d30s \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/users?page=0&size=20
```

### Expected Results
```
Before Optimization:
- Average response: 60-100ms
- Throughput: 150-250 req/s
- Cache hit rate: 0%

After Optimization:
- Average response: 5-15ms
- Throughput: 1000-1500 req/s
- Cache hit rate: 90-95%
```

---

## 📊 Complete Stress Test Suite

### Automated Test Script
Save as `stress-test-suite.sh`:

```bash
#!/bin/bash

echo "========================================="
echo "  E-Commerce API Stress Test Suite"
echo "========================================="
echo ""

# Configuration
BASE_URL="http://localhost:8080"
ADMIN_EMAIL="admin@example.com"
ADMIN_PASSWORD="Admin@123"
CUSTOMER_EMAIL="customer@example.com"
CUSTOMER_PASSWORD="Customer@123"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Authentication
echo "Step 1: Authenticating..."
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"emailAddress\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | jq -r '.data.token')

CUSTOMER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"emailAddress\":\"$CUSTOMER_EMAIL\",\"password\":\"$CUSTOMER_PASSWORD\"}" \
  | jq -r '.data.token')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}✗ Admin authentication failed${NC}"
  exit 1
fi

if [ "$CUSTOMER_TOKEN" == "null" ] || [ -z "$CUSTOMER_TOKEN" ]; then
  echo -e "${RED}✗ Customer authentication failed${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Authentication successful${NC}"
echo ""

# Step 2: Get test data
echo "Step 2: Fetching test data..."
PRODUCT_ID=$(curl -s -X GET "$BASE_URL/api/products?size=1" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  | jq -r '.data.content[0].id')

USER_ID=$(curl -s -X GET "$BASE_URL/api/users?size=1" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.data.content[0].id')

echo -e "${GREEN}✓ Test data retrieved${NC}"
echo "  Product ID: $PRODUCT_ID"
echo "  User ID: $USER_ID"
echo ""

# Step 3: Clear cache for clean test
echo "Step 3: Clearing cache..."
curl -s -X DELETE "$BASE_URL/api/admin/token-cache" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null
echo -e "${GREEN}✓ Cache cleared${NC}"
echo ""

# Step 4: Test 1 - GET Products (Read-Heavy)
echo "========================================="
echo "Test 1: GET /api/products (Read-Heavy)"
echo "========================================="
echo "Running: 1000 requests, 50 concurrent..."

ab -n 1000 -c 50 -q \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  "$BASE_URL/api/products?page=0&size=20" \
  > /tmp/products-test.txt 2>&1

PRODUCTS_RPS=$(grep "Requests per second" /tmp/products-test.txt | awk '{print $4}')
PRODUCTS_TIME=$(grep "Time per request.*mean\)" /tmp/products-test.txt | head -1 | awk '{print $4}')

echo -e "${YELLOW}Results:${NC}"
echo "  Throughput: $PRODUCTS_RPS req/s"
echo "  Avg Response Time: ${PRODUCTS_TIME}ms"
echo ""

# Step 5: Test 2 - GET Order by ID (Cached Read)
echo "========================================="
echo "Test 2: GET /api/orders/{id} (Cached)"
echo "========================================="

# Create test order
echo "Creating test order..."
ORDER_ID=$(curl -s -X POST "$BASE_URL/api/orders" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"items\": [{\"productId\": \"$PRODUCT_ID\", \"quantity\": 1}],
    \"shippingAddressId\": \"550e8400-e29b-41d4-a716-446655440000\",
    \"paymentMethodId\": \"550e8400-e29b-41d4-a716-446655440001\"
  }" | jq -r '.data.id')

echo "Running: 1000 requests, 50 concurrent..."

ab -n 1000 -c 50 -q \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  "$BASE_URL/api/orders/$ORDER_ID" \
  > /tmp/order-get-test.txt 2>&1

ORDER_GET_RPS=$(grep "Requests per second" /tmp/order-get-test.txt | awk '{print $4}')
ORDER_GET_TIME=$(grep "Time per request.*mean\)" /tmp/order-get-test.txt | head -1 | awk '{print $4}')

echo -e "${YELLOW}Results:${NC}"
echo "  Throughput: $ORDER_GET_RPS req/s"
echo "  Avg Response Time: ${ORDER_GET_TIME}ms"
echo ""

# Step 6: Test 3 - POST Orders (Write-Heavy)
echo "========================================="
echo "Test 3: POST /api/orders (Write-Heavy)"
echo "========================================="
echo "Running: 100 requests, 10 concurrent..."

# Create request file
cat > /tmp/order-request.json << EOF
{
  "userId": "$USER_ID",
  "items": [{"productId": "$PRODUCT_ID", "quantity": 1}],
  "shippingAddressId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethodId": "550e8400-e29b-41d4-a716-446655440001"
}
EOF

ab -n 100 -c 10 -q -p /tmp/order-request.json -T application/json \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  "$BASE_URL/api/orders" \
  > /tmp/order-post-test.txt 2>&1

ORDER_POST_RPS=$(grep "Requests per second" /tmp/order-post-test.txt | awk '{print $4}')
ORDER_POST_TIME=$(grep "Time per request.*mean\)" /tmp/order-post-test.txt | head -1 | awk '{print $4}')

echo -e "${YELLOW}Results:${NC}"
echo "  Throughput: $ORDER_POST_RPS req/s"
echo "  Avg Response Time: ${ORDER_POST_TIME}ms"
echo ""

# Step 7: Test 4 - GET Users (Admin Query)
echo "========================================="
echo "Test 4: GET /api/users (Admin Query)"
echo "========================================="
echo "Running: 1000 requests, 50 concurrent..."

ab -n 1000 -c 50 -q \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE_URL/api/users?page=0&size=20" \
  > /tmp/users-test.txt 2>&1

USERS_RPS=$(grep "Requests per second" /tmp/users-test.txt | awk '{print $4}')
USERS_TIME=$(grep "Time per request.*mean\)" /tmp/users-test.txt | head -1 | awk '{print $4}')

echo -e "${YELLOW}Results:${NC}"
echo "  Throughput: $USERS_RPS req/s"
echo "  Avg Response Time: ${USERS_TIME}ms"
echo ""

# Step 8: Get Performance Metrics
echo "========================================="
echo "Performance Metrics"
echo "========================================="

METRICS=$(curl -s -X GET "$BASE_URL/api/admin/token-performance" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

HIT_RATE=$(echo $METRICS | jq -r '.data.hitRate')
PERFORMANCE=$(echo $METRICS | jq -r '.data.performance')
CACHE_SIZE=$(echo $METRICS | jq -r '.data.estimatedSize')

echo -e "${YELLOW}Token Cache Metrics:${NC}"
echo "  Hit Rate: $HIT_RATE"
echo "  Performance: $PERFORMANCE"
echo "  Cache Size: $CACHE_SIZE"
echo ""

# Step 9: Summary Report
echo "========================================="
echo "SUMMARY REPORT"
echo "========================================="
echo ""
echo -e "${GREEN}Endpoint Performance:${NC}"
echo "┌─────────────────────────────────────────────────────────┐"
echo "│ Endpoint              │ Throughput  │ Avg Response Time │"
echo "├─────────────────────────────────────────────────────────┤"
printf "│ GET /api/products     │ %8s    │ %15s   │\n" "$PRODUCTS_RPS" "${PRODUCTS_TIME}ms"
printf "│ GET /api/orders/{id}  │ %8s    │ %15s   │\n" "$ORDER_GET_RPS" "${ORDER_GET_TIME}ms"
printf "│ POST /api/orders      │ %8s    │ %15s   │\n" "$ORDER_POST_RPS" "${ORDER_POST_TIME}ms"
printf "│ GET /api/users        │ %8s    │ %15s   │\n" "$USERS_RPS" "${USERS_TIME}ms"
echo "└─────────────────────────────────────────────────────────┘"
echo ""
echo -e "${GREEN}Cache Performance:${NC}"
echo "  Hit Rate: $HIT_RATE"
echo "  Status: $PERFORMANCE"
echo ""

# Cleanup
rm -f /tmp/products-test.txt /tmp/order-get-test.txt /tmp/order-post-test.txt /tmp/users-test.txt /tmp/order-request.json

echo -e "${GREEN}✓ Stress test suite completed${NC}"
```

### Usage
```bash
chmod +x stress-test-suite.sh
./stress-test-suite.sh
```

---

## 📈 Performance Benchmarks

### Expected Results Summary

| Endpoint | Metric | Before Optimization | After Optimization | Improvement |
|----------|--------|--------------------|--------------------|-------------|
| **GET /api/products** | Throughput | 250 req/s | 1800 req/s | **7.2x** |
| | Avg Response | 60ms | 8ms | **7.5x faster** |
| | Cache Hit Rate | 0% | 92% | **New** |
| **GET /api/orders/{id}** | Throughput | 280 req/s | 1950 req/s | **7x** |
| | Avg Response | 55ms | 2ms | **27.5x faster** |
| | Cache Hit Rate | 0% | 99% | **New** |
| **POST /api/orders** | Throughput | 55 req/s | 85 req/s | **1.5x** |
| | Avg Response | 180ms | 120ms | **1.5x faster** |
| | Success Rate | 96% | 99% | **+3%** |
| **GET /api/users** | Throughput | 220 req/s | 1600 req/s | **7.3x** |
| | Avg Response | 70ms | 10ms | **7x faster** |
| | Cache Hit Rate | 0% | 90% | **New** |

---

## 📝 Performance Report Template

```markdown
# E-Commerce API Performance Test Report

## Test Environment
- **Date:** [Date]
- **Duration:** [Duration]
- **Server:** [Specs]
- **Database:** PostgreSQL 15
- **JVM:** OpenJDK 21
- **Memory:** [RAM]

## Test Configuration
- **Tool:** Apache Bench 2.3
- **Concurrent Users:** 50
- **Total Requests:** 1000 per endpoint
- **Test Duration:** 15 minutes

## Results

### 1. GET /api/products
- **Throughput:** [X] req/s
- **Avg Response Time:** [X]ms
- **P95 Response Time:** [X]ms
- **P99 Response Time:** [X]ms
- **Cache Hit Rate:** [X]%
- **Error Rate:** [X]%

### 2. GET /api/orders/{id}
- **Throughput:** [X] req/s
- **Avg Response Time:** [X]ms
- **Cache Hit Rate:** [X]%

### 3. POST /api/orders
- **Throughput:** [X] req/s
- **Avg Response Time:** [X]ms
- **Success Rate:** [X]%

### 4. GET /api/users
- **Throughput:** [X] req/s
- **Avg Response Time:** [X]ms
- **Cache Hit Rate:** [X]%

## Token Validation Performance
- **Cache Hit Rate:** [X]%
- **Performance Rating:** [EXCELLENT/GOOD/FAIR/POOR]
- **Avg Validation Time (cached):** [X]ms
- **Avg Validation Time (uncached):** [X]ms

## Conclusions
[Your analysis here]

## Recommendations
[Your recommendations here]
```

---

## 🔗 Related Documentation

- [Token Caching Details](TOKEN_CACHING.md)
- [Performance Fixes](PERFORMANCE_FIXES.md)
- [Optimization Summary](TOKEN_OPTIMIZATION_SUMMARY.md)
