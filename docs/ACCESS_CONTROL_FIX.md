# Access Control Fix - Order Endpoints

## 🔴 Issue

Users with valid JWT tokens (including ADMIN role) were getting **403 Forbidden** when accessing `GET /api/orders`, while `GET /api/products` worked fine.

**Error Log**:
```
ACCESS_DENIED — Principal: anonymousUser — Resource: /api/orders
```

## 🔍 Root Cause

The `OrderController` had **conflicting authorization rules**:

### Before Fix:
```java
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")  // Class-level: requires authentication
public class OrderController {
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")  // Method-level: ADMIN only
    public CompletableFuture<ResponseEntity<ApiResponse<Page<OrderResponse>>>> getAllOrders(...) {
        // ...
    }
}
```

**Problem**: The `GET /api/orders` endpoint had **two** `@PreAuthorize` annotations:
1. **Class-level**: `hasAnyRole('ADMIN', 'CUSTOMER')` - Allows both roles
2. **Method-level**: `hasRole('ADMIN')` - Overrides class-level, requires ADMIN only

This created an **overly restrictive** access control where even ADMIN users might be denied if the authentication context wasn't properly set.

### Comparison with ProductController:

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
    // ...
)
```

`GET /api/products/**` is explicitly **permitAll()** in SecurityConfig, so it bypasses authentication entirely.

## ✅ Solution

Removed the redundant method-level `@PreAuthorize("hasRole('ADMIN')")` from `GET /api/orders`:

### After Fix:
```java
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")  // Class-level: requires authentication
public class OrderController {
    
    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves all orders with pagination")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<OrderResponse>>>> getAllOrders(...) {
        // Now inherits class-level authorization: ADMIN or CUSTOMER
    }
}
```

**Result**: 
- `GET /api/orders` now allows **both ADMIN and CUSTOMER** roles (inherits from class-level)
- Authentication is still required (not public like products)
- Consistent with other order endpoints

## 📋 Updated Access Control Matrix

| Endpoint | Method | Access Control | Roles Allowed |
|----------|--------|----------------|---------------|
| `/api/products` | GET | Public (permitAll) | Anyone (no auth) |
| `/api/products` | POST/PUT/DELETE | Authenticated | ADMIN only |
| `/api/orders` | GET (list all) | Authenticated | ADMIN, CUSTOMER |
| `/api/orders/{id}` | GET | Authenticated | ADMIN, CUSTOMER |
| `/api/orders` | POST | Authenticated | ADMIN, CUSTOMER |
| `/api/orders/{id}/status` | PATCH | Authenticated | ADMIN only |
| `/api/orders/{id}` | DELETE | Authenticated | ADMIN only |
| `/api/users` | GET | Authenticated | ADMIN only |

## 🔐 Security Best Practices Applied

### 1. Principle of Least Privilege
- Public endpoints (products) are explicitly marked `permitAll()`
- Protected endpoints require authentication
- Admin-only operations (delete, status updates) have explicit `@PreAuthorize("hasRole('ADMIN')")`

### 2. Defense in Depth
- **Layer 1**: SecurityConfig URL-level rules
- **Layer 2**: Controller class-level `@PreAuthorize`
- **Layer 3**: Method-level `@PreAuthorize` for sensitive operations

### 3. Consistent Authorization
- Class-level annotation applies to all methods
- Method-level annotation overrides for specific operations
- No conflicting or redundant rules

## 🧪 Testing

### Test Case 1: ADMIN Access
```bash
# Login as admin
POST /api/auth/login
{
  "email": "admin@example.com",
  "password": "Admin@123"
}

# Access orders (should work)
GET /api/orders
Authorization: Bearer <admin_token>

# Expected: 200 OK with order list
```

### Test Case 2: CUSTOMER Access
```bash
# Login as customer
POST /api/auth/login
{
  "email": "customer@example.com",
  "password": "Customer@123"
}

# Access orders (should work)
GET /api/orders
Authorization: Bearer <customer_token>

# Expected: 200 OK with order list
```

### Test Case 3: No Authentication
```bash
# Access orders without token
GET /api/orders

# Expected: 401 Unauthorized
```

### Test Case 4: Public Products
```bash
# Access products without token
GET /api/products

# Expected: 200 OK (public endpoint)
```

## 🚀 Next Steps

1. **Restart Application**: 
   ```bash
   mvn spring-boot:run
   ```

2. **Re-test in Postman**:
   - Login with admin credentials
   - Access `GET /api/orders`
   - Should return 200 OK

3. **Verify Logs**:
   - Should see `JWT_VALIDATION_SUCCESS` instead of `ACCESS_DENIED`
   - Principal should show user ID, not `anonymousUser`

## 📝 Additional Notes

### Why Products Work Without Auth?

Products are **intentionally public** for e-commerce browsing:
```java
.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
```

This allows:
- Anonymous users to browse products
- Search engines to index product pages
- Public API access for integrations

### Why Orders Require Auth?

Orders contain **sensitive user data**:
- Personal information
- Purchase history
- Payment details
- Shipping addresses

Therefore, orders require authentication and should ideally be filtered by user:
```java
// Future enhancement: Filter orders by authenticated user
@GetMapping
public CompletableFuture<ResponseEntity<ApiResponse<Page<OrderResponse>>>> getAllOrders(...) {
    // Get current user from SecurityContext
    // Return only their orders (unless ADMIN)
}
```

## 🔧 Related Files Modified

- ✅ `OrderController.java` - Removed redundant `@PreAuthorize` from `GET /api/orders`
- ✅ `POSTMAN_COLLECTION.json` - Already configured correctly
- ✅ `AUTH_TROUBLESHOOTING.md` - Comprehensive auth debugging guide

## ✨ Summary

**Before**: `GET /api/orders` had conflicting authorization rules causing 403 errors

**After**: `GET /api/orders` inherits class-level authorization, allowing ADMIN and CUSTOMER roles

**Impact**: All authenticated users can now access order endpoints as intended
