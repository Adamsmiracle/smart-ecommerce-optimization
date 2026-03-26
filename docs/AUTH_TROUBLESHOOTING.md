# Authentication Troubleshooting Guide

## 🔴 Issue: Getting 401/403 with "anonymousUser" in logs

This means your JWT token is not being sent or recognized by the server.

## ✅ Step-by-Step Fix

### Step 1: Verify Login Response

After running **"0. Setup - Login"**, check the response:

**Expected Response Structure**:
```json
{
  "status": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJyb2xlIjoiQURNSU4iLCJpYXQiOjE3MTE0NTIwMDAsImV4cCI6MTcxMTUzODQwMCwianRpIjoiYWJjZGVmMTIzNDU2In0.signature",
    "refreshToken": "...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "role": "ADMIN",
    "expiresAt": "2026-03-27T09:34:00Z"
  },
  "statusCode": 200
}
```

**Check**:
- ✅ `data.token` exists and is a long string starting with `eyJ`
- ✅ `data.role` is `"ADMIN"` (for admin endpoints)
- ✅ Status code is `200`

### Step 2: Verify Token is Saved

1. Click on collection name **"Smart E-Commerce - Stress Testing"**
2. Go to **Variables** tab
3. Check `jwt_token` variable has a value in **Current Value** column
4. If empty, manually copy token from login response and paste it

### Step 3: Check Authorization Header

For each request (except login):

1. Go to **Authorization** tab
2. **Type** should be: `Inherit auth from parent` (uses collection-level auth)
3. OR manually set:
   - **Type**: `Bearer Token`
   - **Token**: `{{jwt_token}}`

### Step 4: Verify Header is Sent

1. Click **Send** on any protected endpoint
2. Click **Console** (bottom left in Postman)
3. Find your request
4. Check **Request Headers** section
5. Should see: `Authorization: Bearer eyJhbGci...`

**If missing**:
- Authorization is not configured
- Token variable is empty
- Collection-level auth is not inherited

### Step 5: Test Token Validity

Use the **Token Inspect** endpoint:

**Request**:
```
GET http://localhost:8080/api/auth/token/inspect
Authorization: Bearer {{jwt_token}}
```

**Expected Response**:
```json
{
  "status": true,
  "message": "Token is valid",
  "data": {
    "valid": true,
    "subject": "550e8400-e29b-41d4-a716-446655440000",
    "role": "ADMIN",
    "tokenType": "access",
    "issuedAt": "2026-03-26T09:00:00Z",
    "expiresAt": "2026-03-27T09:00:00Z",
    "jti": "abc123",
    "algorithm": "HS256",
    "error": null
  }
}
```

**If `valid: false`**:
- Token is expired (login again)
- Token is blacklisted (logout was called)
- Token signature is invalid (wrong secret key)

---

## 🛠️ Common Issues & Fixes

### Issue 1: Token Not Auto-Saved After Login

**Symptom**: `{{jwt_token}}` variable is empty after login

**Fix**:
1. Open **"0. Setup - Login"** request
2. Go to **Tests** tab
3. Verify this script exists:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.data && jsonData.data.token) {
        pm.collectionVariables.set('jwt_token', jsonData.data.token);
        console.log('JWT Token saved:', jsonData.data.token);
    }
}
```
4. If missing, add it and re-run login

**Manual Fix**:
1. Copy token from login response: `data.token`
2. Collection → Variables → `jwt_token` → Paste in **Current Value**
3. Click **Save**

---

### Issue 2: Authorization Header Not Sent

**Symptom**: Postman Console shows no `Authorization` header

**Fix Option A - Collection Level** (Recommended):
1. Click collection name
2. Go to **Authorization** tab
3. **Type**: `Bearer Token`
4. **Token**: `{{jwt_token}}`
5. Click **Save**
6. All requests will inherit this

**Fix Option B - Request Level**:
1. Open individual request
2. **Authorization** tab
3. Change from `Inherit` to `Bearer Token`
4. **Token**: `{{jwt_token}}`

---

### Issue 3: Token Expired

**Symptom**: 401 error with message "token is invalid or expired"

**Fix**:
1. Re-run **"0. Setup - Login"**
2. New token will be generated and saved
3. Retry your request

**Token Lifetime**: 24 hours (default)

---

### Issue 4: Wrong Role (403 Forbidden)

**Symptom**: 403 error with "Access denied. Insufficient privileges."

**Fix**:
1. Check endpoint requirements:
   - `/api/users` → Requires `ADMIN` role
   - `/api/orders` → Requires `CUSTOMER` or `ADMIN` role
   - `/api/products` (GET) → Public (no auth needed)
   - `/api/products` (POST/PUT/DELETE) → Requires `ADMIN` role

2. Verify your role:
```
GET /api/auth/token/inspect
```
Check `data.role` in response

3. Login with correct credentials:
   - **Admin**: `admin@example.com` / `Admin@123`
   - **Customer**: `customer@example.com` / `Customer@123`

---

### Issue 5: CORS Error (Browser Only)

**Symptom**: Request blocked by CORS policy (only in browser, not Postman)

**Note**: Postman ignores CORS. This only affects browser-based clients.

**Fix**:
1. Check `SecurityConfig.corsConfigurationSource()`
2. Add your frontend origin:
```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "http://localhost:5173",
    "http://your-frontend-url:port"
));
```

---

## 🧪 Quick Test Checklist

Run these in order to verify authentication:

- [ ] **1. Login**: POST `/api/auth/login` → Returns 200 with token
- [ ] **2. Check Variable**: Collection variables → `jwt_token` has value
- [ ] **3. Inspect Token**: GET `/api/auth/token/inspect` → Returns `valid: true`
- [ ] **4. Public Endpoint**: GET `/api/products` → Returns 200 (no auth needed)
- [ ] **5. Protected Endpoint**: GET `/api/orders` → Returns 200 (auth required)
- [ ] **6. Admin Endpoint**: GET `/api/users` → Returns 200 (admin role required)

---

## 📋 Debug Checklist

If still getting `anonymousUser`:

1. **Check Postman Console**:
   - View → Show Postman Console
   - Find your request
   - Verify `Authorization: Bearer ...` header exists

2. **Check Server Logs**:
   - Look for: `JWT_VALIDATION_SUCCESS` or `JWT_VALIDATION_FAILURE`
   - If no JWT logs, token is not reaching the server

3. **Check Token Format**:
   - Should be 3 parts separated by dots: `xxx.yyy.zzz`
   - Each part is Base64-encoded
   - No spaces or line breaks

4. **Check Application Properties**:
   ```properties
   jwt.secret=your-secret-key-here
   jwt.expiration=86400000
   ```

5. **Restart Application**:
   - If you changed security config, restart the Spring Boot app

---

## 🎯 Working Example

**1. Login Request**:
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

**2. Copy Token from Response**:
```json
{
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJyb2xlIjoiQURNSU4ifQ.signature"
  }
}
```

**3. Use Token in Subsequent Requests**:
```http
GET http://localhost:8080/api/orders
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJyb2xlIjoiQURNSU4ifQ.signature
```

---

## 🆘 Still Not Working?

1. **Export and share**:
   - Collection → Export
   - Share the JSON file for review

2. **Check application logs**:
   - Look for `JWT_VALIDATION_FAILURE` messages
   - Check for stack traces

3. **Verify database**:
   - User exists with correct email
   - Password is BCrypt-hashed
   - User has correct role (`ADMIN` or `CUSTOMER`)

4. **Test with cURL**:
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123"}'

# Use token (replace TOKEN with actual value)
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer TOKEN"
```
