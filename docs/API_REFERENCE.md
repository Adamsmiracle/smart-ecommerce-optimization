# Smart E-Commerce API Reference

> **Base URL:** `http://localhost:8080`  
> **Content-Type:** `application/json`  
> **Authentication:** All protected endpoints require `Authorization: Bearer <token>` header

---

## Legend

| Symbol | Meaning |
|--------|---------|
| 🔓 | Public — no token required |
| 🔑 | Requires any valid JWT (ADMIN or CUSTOMER) |
| 👑 | Requires ADMIN role only |
| 🛒 | Requires CUSTOMER role only |

---

## Roles

There are exactly **two roles** in the system:

| Role | Description |
|------|-------------|
| `ADMIN` | Full access to everything — user management, product/category management, all orders, security reports |
| `CUSTOMER` | Access to own data — browse products, manage own cart, place orders, write reviews, manage own addresses and payment methods |

### What Each Role Can Do

| Feature | CUSTOMER | ADMIN |
|---------|:--------:|:-----:|
| Login / Register / Logout | ✅ | ✅ |
| Google OAuth2 Login | ✅ | ✅ |
| Browse products & categories | ✅ | ✅ |
| View a single product / category | ✅ | ✅ |
| Search / filter products | ✅ | ✅ |
| Create / Update / Delete products | ❌ | ✅ |
| Activate / Deactivate products | ❌ | ✅ |
| Update product stock | ❌ | ✅ |
| Create / Update / Delete categories | ❌ | ✅ |
| View own profile | ✅ | ✅ |
| Update own profile | ❌ | ✅ |
| View all users | ❌ | ✅ |
| Search users | ❌ | ✅ |
| Delete / Activate / Deactivate users | ❌ | ✅ |
| View own cart | ✅ | ✅ |
| Add / Update / Remove cart items | ✅ | ✅ |
| Clear cart | ✅ | ✅ |
| View all carts (admin view) | ❌ | ✅ |
| Place an order | ✅ | ✅ |
| View own orders | ✅ | ✅ |
| View all orders | ❌ | ✅ |
| Cancel own order | ✅ | ✅ |
| Update order status / payment status | ❌ | ✅ |
| Delete orders | ❌ | ✅ |
| Top customers report | ❌ | ✅ |
| Submit / Update / Delete reviews | ✅ | ✅ |
| View reviews | ✅ | ✅ |
| Manage own addresses | ✅ | ✅ |
| View all addresses | ✅ | ✅ |
| Manage own payment methods | ✅ | ✅ |
| View shipping methods | ✅ | ✅ |
| Create / Update / Delete shipping methods | ❌ | ✅ |
| Security audit report | ❌ | ✅ |

### How to Check the Logged-in User's Role

The role is returned in the login/register/OAuth2 response:

```json
{
  "data": {
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "role": "ADMIN",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**Frontend implementation pattern:**
```js
// After login, store role alongside token
const { token, role, userId } = response.data.data;
localStorage.setItem('token', token);
localStorage.setItem('role', role);
localStorage.setItem('userId', userId);

// Guard UI elements based on role
const isAdmin = localStorage.getItem('role') === 'ADMIN';
const isCustomer = localStorage.getItem('role') === 'CUSTOMER';

// Example: only show "Create Product" button to admins
if (isAdmin) {
  showCreateProductButton();
}
```

> ⚠️ **Important:** Always enforce access on the **server side** too. Hiding a button in the UI is cosmetic only — the API will reject unauthorized requests with `403 Forbidden` regardless.

---

## Standard Response Wrapper

Every response is wrapped in this envelope:

```json
{
  "status": true,
  "message": "Success",
  "data": { ... },
  "statusCode": 200,
  "timestamp": "2026-03-03T13:00:00Z"
}
```

**Error response:**
```json
{
  "status": false,
  "message": "Error description",
  "statusCode": 400
}
```

**Paginated response (`data` field):**
```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 10,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false
}
```

**Pagination query params** (available on all list endpoints):
| Param | Default | Example |
|-------|---------|---------|
| `page` | `0` | `?page=1` |
| `size` | `10` | `?size=20` |
| `sort` | `createdAt,asc` | `?sort=name,asc` |

---

## 1. Authentication

### 🔓 POST `/api/auth/login`
Login with email and password. Returns a JWT token.

**Request Body:**
```json
{
  "email": "admin@smartecommerce.com",
  "password": "password123"
}
```

**200 Response:**
```json
{
  "status": true,
  "data": {
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "role": "ADMIN",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**401 Response:**
```json
{
  "status": false,
  "message": "Invalid credentials",
  "statusCode": 401
}
```

---

### 🔓 POST `/api/auth/register`
Register a new user account. Returns a JWT token immediately.

**Request Body:**
```json
{
  "emailAddress": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "password": "password123",
  "role": "CUSTOMER"
}
```
> `role` defaults to `"CUSTOMER"` if omitted. `firstName`, `lastName`, `phoneNumber` are optional.

**201 Response:**
```json
{
  "status": true,
  "message": "User registered successfully",
  "data": {
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "role": "CUSTOMER",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

### 🔑 POST `/api/auth/logout`
Invalidates the current JWT token (adds it to the blacklist).

**Headers:** `Authorization: Bearer <token>`

**Request Body:** _(none)_

**200 Response:**
```json
{
  "status": true,
  "message": "Logged out successfully"
}
```

---

### 🔓 GET `/oauth2/authorization/google`
Initiates Google OAuth2 login. **Open this URL in the browser** — it redirects to Google's login page.

**No request body.**

**Success — browser receives:**
```json
{
  "status": true,
  "message": "OAuth2 authentication successful",
  "data": {
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "role": "CUSTOMER",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "user@gmail.com"
  }
}
```

---

## 2. Users

### 🔑 POST `/api/users`
Create a new user.

**Request Body:**
```json
{
  "emailAddress": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "password": "password123",
  "role": "CUSTOMER"
}
```

**201 Response:**
```json
{
  "status": true,
  "message": "User created successfully",
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "emailAddress": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "isActive": true,
    "role": "CUSTOMER",
    "createdAt": "2026-03-03T13:00:00Z",
    "updatedAt": "2026-03-03T13:00:00Z"
  }
}
```

---

### 🔑 GET `/api/users/{id}`
Get a user by UUID.

**Path Param:** `id` — UUID

**200 Response:**
```json
{
  "status": true,
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "emailAddress": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "isActive": true,
    "role": "CUSTOMER",
    "createdAt": "2026-03-03T13:00:00Z",
    "updatedAt": "2026-03-03T13:00:00Z"
  }
}
```

---

### 🔑 GET `/api/users/email/{email}`
Get a user by email address.

**Path Param:** `email` — string

**200 Response:** _(same as Get User by ID)_

---

### 👑 GET `/api/users`
Get all users, paginated.

**Query Params:** `page`, `size`, `sort`

**200 Response:**
```json
{
  "status": true,
  "data": {
    "content": [ { ...UserResponse }, ... ],
    "totalElements": 50,
    "totalPages": 5,
    "number": 0,
    "size": 10
  }
}
```

---

### 👑 GET `/api/users/search`
Search users by name or email.

**Query Params:**
| Param | Required | Example |
|-------|----------|---------|
| `keyword` | ✅ | `?keyword=john` |
| `page` | ❌ | `?page=0` |
| `size` | ❌ | `?size=10` |

**200 Response:** _(paginated UserResponse list)_

---

### 👑 PUT `/api/users/{id}`
Update a user.

**Path Param:** `id` — UUID

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Updated",
  "phoneNumber": "+1987654321"
}
```

**200 Response:** _(UserResponse with updated fields)_

---

### 👑 DELETE `/api/users/{id}`
Delete a user.

**200 Response:**
```json
{ "status": true, "message": "User deleted successfully" }
```

---

### 👑 POST `/api/users/{id}/activate`
Activate a deactivated user account.

**200 Response:**
```json
{ "status": true, "message": "User activated successfully" }
```

---

### 👑 POST `/api/users/{id}/deactivate`
Deactivate a user account.

**200 Response:**
```json
{ "status": true, "message": "User deactivated successfully" }
```

---

## 3. Products

### 🔓 GET `/api/products`
Get all products, paginated.

**Query Params:** `page`, `size`, `sort`

**200 Response:**
```json
{
  "status": true,
  "data": {
    "content": [
      {
        "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "categoryId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "name": "Laptop Pro 15\"",
        "description": "High-performance laptop",
        "price": 1299.99,
        "stockQuantity": 50,
        "isActive": true,
        "inStock": true,
        "images": ["https://example.com/image1.jpg"],
        "createdAt": "2026-03-03T13:00:00Z",
        "updatedAt": "2026-03-03T13:00:00Z"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

---

### 🔓 GET `/api/products/{id}`
Get a single product by UUID.

**200 Response:** _(single ProductResponse)_

---

### 🔓 GET `/api/products/active`
Get only active products, paginated.

**200 Response:** _(paginated ProductResponse list)_

---

### 🔓 GET `/api/products/category/{categoryId}`
Get products by category UUID.

**Path Param:** `categoryId` — UUID

**200 Response:** _(paginated ProductResponse list)_

---

### 🔓 GET `/api/products/search`
Search products by keyword.

**Query Params:**
| Param | Required | Example |
|-------|----------|---------|
| `keyword` | ✅ | `?keyword=laptop` |
| `page` | ❌ | `?page=0` |
| `size` | ❌ | `?size=10` |

**200 Response:** _(paginated ProductResponse list)_

---

### 🔓 GET `/api/products/price-range`
Get products within a price range.

**Query Params:**
| Param | Required | Example |
|-------|----------|---------|
| `minPrice` | ✅ | `?minPrice=10.00` |
| `maxPrice` | ✅ | `?maxPrice=500.00` |
| `page` | ❌ | `?page=0` |
| `size` | ❌ | `?size=10` |

**200 Response:** _(paginated ProductResponse list)_

---

### 🔓 GET `/api/products/in-stock`
Get products that are in stock.

**200 Response:** _(paginated ProductResponse list)_

---

### 👑 POST `/api/products`
Create a new product.

**Request Body:**
```json
{
  "categoryId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "price": 29.99,
  "stockQuantity": 200,
  "isActive": true,
  "images": ["https://example.com/mouse.jpg"]
}
```

**201 Response:** _(ProductResponse)_

---

### 👑 PUT `/api/products/{id}`
Update a product.

**Request Body:** _(same fields as create, all optional)_

**200 Response:** _(updated ProductResponse)_

---

### 👑 DELETE `/api/products/{id}`
Delete a product.

**200 Response:**
```json
{ "status": true, "message": "Product deleted successfully" }
```

---

### 👑 POST `/api/products/{id}/activate`
Activate a product (makes it visible).

**200 Response:**
```json
{ "status": true, "message": "Product activated successfully" }
```

---

### 👑 POST `/api/products/{id}/deactivate`
Deactivate a product (hides it from listings).

**200 Response:**
```json
{ "status": true, "message": "Product deactivated successfully" }
```

---

### 👑 PATCH `/api/products/{id}/stock`
Update product stock quantity.

**Query Params:**
| Param | Required | Description |
|-------|----------|-------------|
| `quantity` | ✅ | Units to add (use negative to reduce) |

**200 Response:**
```json
{ "status": true, "message": "Stock updated successfully" }
```

---

## 4. Categories

### 🔓 GET `/api/categories`
Get all categories, paginated.

**200 Response:**
```json
{
  "status": true,
  "data": {
    "content": [
      {
        "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "categoryName": "Electronics",
        "createdAt": "2026-03-03T13:00:00Z",
        "updatedAt": "2026-03-03T13:00:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

---

### 🔓 GET `/api/categories/{id}`
Get a single category by UUID.

**200 Response:** _(single CategoryResponse)_

---

### 👑 POST `/api/categories`
Create a new category.

**Request Body:**
```json
{
  "categoryName": "Electronics"
}
```

**201 Response:** _(CategoryResponse)_

---

### 👑 PUT `/api/categories/{id}`
Update a category.

**Request Body:**
```json
{
  "categoryName": "Consumer Electronics"
}
```

**200 Response:** _(updated CategoryResponse)_

---

### 👑 DELETE `/api/categories/{id}`
Delete a category.

**200 Response:**
```json
{ "status": true, "message": "Category deleted successfully" }
```

---

## 5. Cart

### 🔑 GET `/api/cart/user/{userId}`
Get a user's shopping cart.

**Path Param:** `userId` — UUID

**200 Response:**
```json
{
  "status": true,
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "items": [
      {
        "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "productId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 29.99,
        "totalPrice": 59.98
      }
    ],
    "totalItems": 2,
    "totalPrice": 59.98,
    "createdAt": "2026-03-03T13:00:00Z"
  }
}
```

---

### 🔑 POST `/api/cart/user/{userId}/items`
Add an item to the cart.

**Path Param:** `userId` — UUID

**Request Body:**
```json
{
  "productId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "quantity": 2
}
```

**200 Response:** _(CartResponse with updated items)_

---

### 🔑 PUT `/api/cart/user/{userId}/items/{itemId}`
Update the quantity of a cart item.

**Path Params:** `userId`, `itemId` — UUIDs

**Query Params:**
| Param | Required | Description |
|-------|----------|-------------|
| `quantity` | ✅ | New quantity (integer ≥ 1) |

**200 Response:** _(updated CartResponse)_

---

### 🔑 DELETE `/api/cart/user/{userId}/items/{itemId}`
Remove a specific item from the cart.

**200 Response:** _(CartResponse without removed item)_

---

### 🔑 DELETE `/api/cart/user/{userId}`
Clear all items from the cart.

**200 Response:**
```json
{ "status": true, "message": "Cart cleared successfully" }
```

---

### 🔑 GET `/api/cart/user/{userId}/count`
Get total number of items in the cart.

**200 Response:**
```json
{ "status": true, "data": 3 }
```

---

### 🔑 GET `/api/cart/count`
Same as above but with query param instead of path param.

**Query Params:**
| Param | Required |
|-------|----------|
| `userId` | ✅ |

---

### 👑 GET `/api/cart`
Get all carts (admin view), paginated.

**200 Response:** _(paginated CartResponse list)_

---

## 6. Orders

### 🔑 POST `/api/orders`
Place a new order (checkout).

**Request Body:**
```json
{
  "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "paymentMethodId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "shippingAddressId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "shippingMethodId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "customerNotes": "Please leave at door",
  "items": [
    {
      "productId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "quantity": 2
    }
  ]
}
```
> `paymentMethodId`, `shippingAddressId`, `shippingMethodId`, `customerNotes` are optional.

**201 Response:**
```json
{
  "status": true,
  "message": "Order placed successfully",
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "customerName": "John Doe",
    "orderNumber": "ORD-2026-001",
    "status": "PENDING",
    "paymentMethodId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "shippingMethodId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "subtotal": 59.98,
    "shippingCost": 5.99,
    "total": 65.97,
    "itemCount": 2,
    "customerNotes": "Please leave at door",
    "createdAt": "2026-03-03T13:00:00Z",
    "cancelledAt": null,
    "items": [
      {
        "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "productId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "productName": "Wireless Mouse",
        "unitPrice": 29.99,
        "quantity": 2,
        "totalPrice": 59.98
      }
    ],
    "shippingAddress": {
      "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "addressLine": "123 Main St",
      "city": "New York",
      "region": "NY",
      "country": "USA",
      "postalCode": "10001",
      "fullAddress": "123 Main St, New York, NY, 10001, USA"
    },
    "shippingMethod": {
      "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "name": "Standard Shipping",
      "price": 5.99,
      "estimatedDays": 5
    }
  }
}
```

---

### 🔑 GET `/api/orders/{id}`
Get an order by UUID.

**200 Response:** _(single OrderResponse)_

---

### 🔑 GET `/api/orders/number/{orderNumber}`
Get an order by its order number (e.g., `ORD-2026-001`).

**200 Response:** _(single OrderResponse)_

---

### 🔑 GET `/api/orders/user/{userId}`
Get all orders for a specific user, paginated.

> 💡 **For CUSTOMERS:** Use this endpoint to fetch your own orders. Pass your `userId` (received at login).

**Path Param:** `userId` — UUID

**200 Response:** _(paginated OrderResponse list)_

**Example Request:**
```js
// Get logged-in customer's orders
const userId = localStorage.getItem('userId');
fetch(`http://localhost:8080/api/orders/user/${userId}?page=0&size=20`, {
  headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
})
```

---

### 👑 GET `/api/orders`
Get **ALL orders** in the system (across all users), paginated.

> ⚠️ **ADMIN only.** Customers cannot access this — use `/api/orders/user/{userId}` instead.

**200 Response:** _(paginated OrderResponse list)_

---

### 👑 GET `/api/orders/status/{status}`
Get orders filtered by status.

**Path Param:** `status` — one of: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`

**200 Response:** _(paginated OrderResponse list)_

---

### 🔑 POST `/api/orders/{id}/cancel`
Cancel an order.

**200 Response:**
```json
{
  "status": true,
  "message": "Order cancelled successfully",
  "data": { ...OrderResponse with status: "CANCELLED" }
}
```

---

### 👑 PATCH `/api/orders/{id}/status`
Update the status of an order.

**Query Params:**
| Param | Required | Values |
|-------|----------|--------|
| `status` | ✅ | `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

**200 Response:** _(updated OrderResponse)_

---

### 👑 PATCH `/api/orders/{id}/payment-status`
Update the payment status of an order.

**Query Params:**
| Param | Required | Values |
|-------|----------|--------|
| `paymentStatus` | ✅ | `PENDING`, `PAID`, `FAILED`, `REFUNDED` |

**200 Response:** _(updated OrderResponse)_

---

### 👑 PUT `/api/orders/{id}`
Update editable order fields.

**Request Body:**
```json
{
  "customerNotes": "Updated delivery instructions",
  "shippingAddressId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**200 Response:** _(updated OrderResponse)_

---

### 👑 DELETE `/api/orders/{id}`
Delete an order.

**200 Response:**
```json
{ "status": true, "message": "Order deleted successfully" }
```

---

### 👑 GET `/api/orders/count`
Get total number of orders.

**200 Response:**
```json
{ "status": true, "data": 142 }
```

---

### 👑 GET `/api/orders/count/status/{status}`
Get number of orders with a specific status.

**200 Response:**
```json
{ "status": true, "data": 23 }
```

---

### 👑 GET `/api/orders/reports/top-customers`
Get top customers by total spending.

**Query Params:**
| Param | Default | Description |
|-------|---------|-------------|
| `limit` | `10` | Number of customers to return |

**200 Response:**
```json
{
  "status": true,
  "data": [
    {
      "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "customerName": "John Doe",
      "email": "john@example.com",
      "totalOrders": 12,
      "totalSpent": 4599.88
    }
  ]
}
```

---

## 7. Reviews

### 🔑 POST `/api/reviews`
Submit a product review.

**Request Body:**
```json
{
  "productId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "rating": 5,
  "comment": "Excellent product, highly recommend!"
}
```
> `rating` must be between 1–5. `comment` is optional (max 2000 chars). One review per user per product.

**201 Response:**
```json
{
  "status": true,
  "message": "Review submitted successfully",
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "productId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "rating": 5,
    "comment": "Excellent product!",
    "createdAt": "2026-03-03T13:00:00Z",
    "updatedAt": "2026-03-03T13:00:00Z"
  }
}
```

---

### 🔑 GET `/api/reviews`
Get all reviews, paginated.

**200 Response:** _(paginated ReviewResponse list)_

---

### 🔑 GET `/api/reviews/{id}`
Get a review by UUID.

**200 Response:** _(single ReviewResponse)_

---

### 🔑 GET `/api/reviews/product/{productId}`
Get all reviews for a product, paginated.

**200 Response:** _(paginated ReviewResponse list)_

---

### 🔑 GET `/api/reviews/user/{userId}`
Get all reviews submitted by a user, paginated.

**200 Response:** _(paginated ReviewResponse list)_

---

### 🔑 GET `/api/reviews/product/{productId}/average-rating`
Get the average rating for a product.

**200 Response:**
```json
{ "status": true, "data": 4.3 }
```

---

### 🔑 GET `/api/reviews/product/{productId}/count`
Get the total number of reviews for a product.

**200 Response:**
```json
{ "status": true, "data": 28 }
```

---

### 🔑 GET `/api/reviews/check`
Check if a user has already reviewed a product.

**Query Params:**
| Param | Required |
|-------|----------|
| `userId` | ✅ |
| `productId` | ✅ |

**200 Response:**
```json
{ "status": true, "data": true }
```

---

### 🔑 PUT `/api/reviews/{id}`
Update a review.

**Request Body:** _(same as create)_

**200 Response:** _(updated ReviewResponse)_

---

### 🔑 DELETE `/api/reviews/{id}`
Delete a review.

**200 Response:**
```json
{ "status": true, "message": "Review deleted successfully" }
```

---

## 8. Addresses

### 🔑 POST `/api/addresses`
Create a new address for a user.

**Request Body:**
```json
{
  "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "addressLine": "123 Main St",
  "city": "New York",
  "region": "NY",
  "postalCode": "10001",
  "country": "USA",
  "addressType": "HOME",
  "isDefault": true
}
```

**201 Response:**
```json
{
  "status": true,
  "message": "Address created successfully",
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "addressLine": "123 Main St",
    "city": "New York",
    "region": "NY",
    "postalCode": "10001",
    "country": "USA",
    "addressType": "HOME",
    "isDefault": true,
    "createdAt": "2026-03-03T13:00:00Z"
  }
}
```

---

### 🔑 GET `/api/addresses/{id}`
Get an address by UUID.

**200 Response:** _(single AddressResponse)_

---

### 🔑 GET `/api/addresses`
Get all addresses (admin view), paginated.

**200 Response:** _(paginated AddressResponse list)_

---

### 🔑 GET `/api/addresses/user/{userId}`
Get all addresses for a user, paginated.

**Query Params:** `page` (default 0), `size` (default 10)

**200 Response:** _(paginated AddressResponse list)_

---

### 🔑 GET `/api/addresses/user/{userId}/shipping`
Get all shipping addresses for a user.

**Query Params:** `page` (default 0), `size` (default 10)

**200 Response:** _(paginated AddressResponse list)_

---

### 🔑 GET `/api/addresses/user/{userId}/billing`
Get all billing addresses for a user.

**Query Params:** `page` (default 0), `size` (default 10)

**200 Response:** _(paginated AddressResponse list)_

---

### 🔑 PUT `/api/addresses/{id}`
Update an address.

**Request Body:** _(same as create)_

**200 Response:** _(updated AddressResponse)_

---

### 🔑 DELETE `/api/addresses/{id}`
Delete an address.

**200 Response:**
```json
{ "status": true, "message": "Address deleted successfully" }
```

---

## 9. Payment Methods

### 🔑 POST `/api/payment-methods`
Add a payment method for a user.

**Request Body:**
```json
{
  "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "paymentType": "CREDIT_CARD",
  "provider": "Visa",
  "accountNumber": "1234",
  "expiryDate": "2028-12-31T23:59:59Z",
  "isDefault": true,
  "isActive": true
}
```

**200 Response:**
```json
{
  "status": true,
  "message": "Payment method created",
  "data": {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "paymentType": "CREDIT_CARD",
    "provider": "Visa",
    "accountNumber": "1234",
    "expiryDate": "2028-12-31T23:59:59Z",
    "isDefault": true,
    "isActive": true,
    "createdAt": "2026-03-03T13:00:00Z"
  }
}
```

---

### 🔑 GET `/api/payment-methods/{id}`
Get a payment method by UUID.

**200 Response:** _(single PaymentMethodResponse)_

---

### 🔑 GET `/api/payment-methods/user/{userId}`
Get all payment methods for a user, paginated.

**200 Response:** _(paginated PaymentMethodResponse list)_

---

### 🔑 PUT `/api/payment-methods/{id}`
Update a payment method.

**Request Body:** _(same as create)_

**200 Response:** _(updated PaymentMethodResponse)_

---

### 🔑 DELETE `/api/payment-methods/{id}`
Delete a payment method.

**200 Response:**
```json
{ "status": true, "message": "Payment method deleted" }
```

---

## 10. Shipping Methods

### 🔑 GET `/api/shipping-methods`
Get all shipping methods, paginated.

**200 Response:**
```json
{
  "status": true,
  "data": {
    "content": [
      {
        "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "name": "Standard Shipping",
        "description": "Standard ground shipping",
        "price": 5.99,
        "estimatedDays": 5,
        "isActive": true,
        "createdAt": "2026-03-03T13:00:00Z"
      }
    ]
  }
}
```

---

### 🔑 GET `/api/shipping-methods/{id}`
Get a shipping method by UUID.

**200 Response:** _(single ShippingMethodResponse)_

---

### 👑 POST `/api/shipping-methods`
Create a new shipping method.

**Request Body:**
```json
{
  "name": "Overnight Shipping",
  "description": "Next day delivery",
  "price": 25.99,
  "estimatedDays": 1,
  "isActive": true
}
```

**200 Response:** _(ShippingMethodResponse)_

---

### 👑 PUT `/api/shipping-methods/{id}`
Update a shipping method.

**Request Body:** _(same as create)_

**200 Response:** _(updated ShippingMethodResponse)_

---

### 👑 DELETE `/api/shipping-methods/{id}`
Delete a shipping method.

**200 Response:**
```json
{ "status": true, "message": "Shipping method deleted" }
```

---

## 11. Admin

### 👑 GET `/api/admin/security-report`
Get the live security audit report.

**200 Response:**
```json
{
  "status": true,
  "data": {
    "authenticationStats": {
      "successCount": 42,
      "failureCount": 3,
      "deniedCount": 1
    },
    "tokenStats": {
      "activeTokenCount": 12,
      "totalValidations": 156,
      "blacklistedCount": 4
    },
    "recentSecurityEvents": [
      {
        "type": "AUTH_SUCCESS",
        "principal": "admin@smartecommerce.com",
        "detail": "Authentication succeeded",
        "timestamp": "2026-03-03T13:00:00Z"
      },
      {
        "type": "AUTH_FAILURE",
        "principal": "unknown@example.com",
        "detail": "Bad credentials",
        "timestamp": "2026-03-03T12:59:00Z"
      }
    ]
  }
}
```

---

## 12. Error Codes Reference

| HTTP Status | Meaning | When it happens |
|-------------|---------|-----------------|
| `200` | OK | Request succeeded |
| `201` | Created | Resource created successfully |
| `400` | Bad Request | Validation failed, invalid UUID, missing required fields |
| `401` | Unauthorized | No token, expired token, or invalid token |
| `403` | Forbidden | Valid token but insufficient role (e.g. CUSTOMER hitting admin endpoint) |
| `404` | Not Found | Resource with given ID does not exist |
| `409` | Conflict | Duplicate resource (e.g. email already registered) |
| `500` | Internal Server Error | Unexpected server error |

**401 response body:**
```json
{
  "status": false,
  "message": "Authentication required. Provide a valid Bearer JWT token.",
  "statusCode": 401
}
```

**403 response body:**
```json
{
  "status": false,
  "message": "Access denied. Insufficient role privileges.",
  "statusCode": 403
}
```

**404 response body:**
```json
{
  "status": false,
  "message": "User not found with id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "statusCode": 404
}
```

---

## 13. Quick Reference — All Endpoints

| Method | Endpoint | Auth | Who Can Access | Description |
|--------|----------|:----:|----------------|-------------|
| POST | `/api/auth/login` | 🔓 | Everyone | Login |
| POST | `/api/auth/register` | 🔓 | Everyone | Register |
| POST | `/api/auth/logout` | 🔑 | ADMIN, CUSTOMER | Logout |
| GET | `/oauth2/authorization/google` | 🔓 | Everyone | Google OAuth2 login |
| POST | `/api/users` | 🔑 | ADMIN, CUSTOMER | Create user |
| GET | `/api/users` | 👑 | ADMIN only | List all users |
| GET | `/api/users/{id}` | 🔑 | ADMIN, CUSTOMER | Get user by ID |
| GET | `/api/users/email/{email}` | 🔑 | ADMIN, CUSTOMER | Get user by email |
| GET | `/api/users/search` | 👑 | ADMIN only | Search users |
| PUT | `/api/users/{id}` | 👑 | ADMIN only | Update user |
| DELETE | `/api/users/{id}` | 👑 | ADMIN only | Delete user |
| POST | `/api/users/{id}/activate` | 👑 | ADMIN only | Activate user |
| POST | `/api/users/{id}/deactivate` | 👑 | ADMIN only | Deactivate user |
| GET | `/api/products` | 🔓 | Everyone | List all products |
| GET | `/api/products/{id}` | 🔓 | Everyone | Get product by ID |
| GET | `/api/products/active` | 🔓 | Everyone | Active products only |
| GET | `/api/products/category/{categoryId}` | 🔓 | Everyone | Products by category |
| GET | `/api/products/search` | 🔓 | Everyone | Search products |
| GET | `/api/products/price-range` | 🔓 | Everyone | Products by price range |
| GET | `/api/products/in-stock` | 🔓 | Everyone | In-stock products |
| POST | `/api/products` | 👑 | ADMIN only | Create product |
| PUT | `/api/products/{id}` | 👑 | ADMIN only | Update product |
| DELETE | `/api/products/{id}` | 👑 | ADMIN only | Delete product |
| POST | `/api/products/{id}/activate` | 👑 | ADMIN only | Activate product |
| POST | `/api/products/{id}/deactivate` | 👑 | ADMIN only | Deactivate product |
| PATCH | `/api/products/{id}/stock` | 👑 | ADMIN only | Update stock quantity |
| GET | `/api/categories` | 🔓 | Everyone | List all categories |
| GET | `/api/categories/{id}` | 🔓 | Everyone | Get category by ID |
| POST | `/api/categories` | 👑 | ADMIN only | Create category |
| PUT | `/api/categories/{id}` | 👑 | ADMIN only | Update category |
| DELETE | `/api/categories/{id}` | 👑 | ADMIN only | Delete category |
| GET | `/api/cart` | 👑 | ADMIN only | View all carts |
| GET | `/api/cart/user/{userId}` | 🔑 | ADMIN, CUSTOMER | Get user's cart |
| POST | `/api/cart/user/{userId}/items` | 🔑 | ADMIN, CUSTOMER | Add item to cart |
| PUT | `/api/cart/user/{userId}/items/{itemId}` | 🔑 | ADMIN, CUSTOMER | Update cart item quantity |
| DELETE | `/api/cart/user/{userId}/items/{itemId}` | 🔑 | ADMIN, CUSTOMER | Remove item from cart |
| DELETE | `/api/cart/user/{userId}` | 🔑 | ADMIN, CUSTOMER | Clear cart |
| GET | `/api/cart/user/{userId}/count` | 🔑 | ADMIN, CUSTOMER | Get cart item count |
| GET | `/api/cart/count` | 🔑 | ADMIN, CUSTOMER | Get cart item count (query param) |
| POST | `/api/orders` | 🔑 | ADMIN, CUSTOMER | Place an order |
| GET | `/api/orders` | 👑 | ADMIN only | List all orders |
| GET | `/api/orders/{id}` | 🔑 | ADMIN, CUSTOMER | Get order by ID |
| GET | `/api/orders/number/{orderNumber}` | 🔑 | ADMIN, CUSTOMER | Get order by order number |
| GET | `/api/orders/user/{userId}` | 🔑 | ADMIN, CUSTOMER | Get orders for a user |
| GET | `/api/orders/status/{status}` | 👑 | ADMIN only | Filter orders by status |
| POST | `/api/orders/{id}/cancel` | 🔑 | ADMIN, CUSTOMER | Cancel an order |
| PATCH | `/api/orders/{id}/status` | 👑 | ADMIN only | Update order status |
| PATCH | `/api/orders/{id}/payment-status` | 👑 | ADMIN only | Update payment status |
| PUT | `/api/orders/{id}` | 👑 | ADMIN only | Update order fields |
| DELETE | `/api/orders/{id}` | 👑 | ADMIN only | Delete order |
| GET | `/api/orders/count` | 👑 | ADMIN only | Total order count |
| GET | `/api/orders/count/status/{status}` | 👑 | ADMIN only | Order count by status |
| GET | `/api/orders/reports/top-customers` | 👑 | ADMIN only | Top customers by spending |
| POST | `/api/reviews` | 🔑 | ADMIN, CUSTOMER | Submit a review |
| GET | `/api/reviews` | 🔑 | ADMIN, CUSTOMER | List all reviews |
| GET | `/api/reviews/{id}` | 🔑 | ADMIN, CUSTOMER | Get review by ID |
| GET | `/api/reviews/product/{productId}` | 🔑 | ADMIN, CUSTOMER | Reviews for a product |
| GET | `/api/reviews/user/{userId}` | 🔑 | ADMIN, CUSTOMER | Reviews by a user |
| GET | `/api/reviews/product/{productId}/average-rating` | 🔑 | ADMIN, CUSTOMER | Average product rating |
| GET | `/api/reviews/product/{productId}/count` | 🔑 | ADMIN, CUSTOMER | Total review count |
| GET | `/api/reviews/check` | 🔑 | ADMIN, CUSTOMER | Has user reviewed product? |
| PUT | `/api/reviews/{id}` | 🔑 | ADMIN, CUSTOMER | Update a review |
| DELETE | `/api/reviews/{id}` | 🔑 | ADMIN, CUSTOMER | Delete a review |
| POST | `/api/addresses` | 🔑 | ADMIN, CUSTOMER | Create address |
| GET | `/api/addresses` | 🔑 | ADMIN, CUSTOMER | List all addresses |
| GET | `/api/addresses/{id}` | 🔑 | ADMIN, CUSTOMER | Get address by ID |
| GET | `/api/addresses/user/{userId}` | 🔑 | ADMIN, CUSTOMER | Get user's addresses |
| GET | `/api/addresses/user/{userId}/shipping` | 🔑 | ADMIN, CUSTOMER | Get shipping addresses |
| GET | `/api/addresses/user/{userId}/billing` | 🔑 | ADMIN, CUSTOMER | Get billing addresses |
| PUT | `/api/addresses/{id}` | 🔑 | ADMIN, CUSTOMER | Update address |
| DELETE | `/api/addresses/{id}` | 🔑 | ADMIN, CUSTOMER | Delete address |
| POST | `/api/payment-methods` | 🔑 | ADMIN, CUSTOMER | Add payment method |
| GET | `/api/payment-methods/{id}` | 🔑 | ADMIN, CUSTOMER | Get payment method |
| GET | `/api/payment-methods/user/{userId}` | 🔑 | ADMIN, CUSTOMER | List user payment methods |
| PUT | `/api/payment-methods/{id}` | 🔑 | ADMIN, CUSTOMER | Update payment method |
| DELETE | `/api/payment-methods/{id}` | 🔑 | ADMIN, CUSTOMER | Delete payment method |
| POST | `/api/shipping-methods` | 👑 | ADMIN only | Create shipping method |
| GET | `/api/shipping-methods` | 🔑 | ADMIN, CUSTOMER | List shipping methods |
| GET | `/api/shipping-methods/{id}` | 🔑 | ADMIN, CUSTOMER | Get shipping method |
| PUT | `/api/shipping-methods/{id}` | 👑 | ADMIN only | Update shipping method |
| DELETE | `/api/shipping-methods/{id}` | 👑 | ADMIN only | Delete shipping method |
| GET | `/api/admin/security-report` | 👑 | ADMIN only | Security audit report |

---

## 14. Common Frontend Mistakes & Troubleshooting

### ❌ Mistake 1: Using the wrong endpoint for CUSTOMERS

**Problem:** CUSTOMER tries to fetch all orders:
```js
fetch('http://localhost:8080/api/orders')  // ❌ ADMIN only endpoint
```
**Result:** `403 Access Denied`

**Fix:** Use the user-specific endpoint:
```js
const userId = localStorage.getItem('userId');
fetch(`http://localhost:8080/api/orders/user/${userId}`)  // ✅ Correct
```

**Affected endpoints:**
| Wrong (ADMIN-only) | Correct (user-specific) |
|-------------------|-------------------------|
| `GET /api/orders` | `GET /api/orders/user/{userId}` |
| `GET /api/users` | `GET /api/users/{userId}` or `GET /api/users/email/{email}` |
| `GET /api/cart` | `GET /api/cart/user/{userId}` |

---

### ❌ Mistake 2: Forgetting to include the Authorization header

**Problem:**
```js
fetch('http://localhost:8080/api/cart/user/xyz')  // ❌ No token
```
**Result:** `401 Authentication required`

**Fix:**
```js
fetch('http://localhost:8080/api/cart/user/xyz', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`  // ✅
  }
})
```

---

### ❌ Mistake 3: Token expired

**Problem:** Token was issued 25 hours ago (default expiry: 24 hours).

**Result:** `401 Authentication required`

**Fix:** Prompt the user to log in again. Optionally, implement token refresh (not currently in the API).

---

### ❌ Mistake 4: Hardcoding the wrong userId

**Problem:**
```js
// ❌ Wrong — using someone else's ID
fetch('http://localhost:8080/api/cart/user/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx')
```

**Fix:** Always use the logged-in user's ID:
```js
const userId = localStorage.getItem('userId');  // From login response
fetch(`http://localhost:8080/api/cart/user/${userId}`)
```

---

### ❌ Mistake 5: Trying to access ADMIN endpoints as CUSTOMER

**Problem:**
```js
// CUSTOMER token, but trying to create a product
fetch('http://localhost:8080/api/products', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${customerToken}` },
  body: JSON.stringify({ name: 'New Product', ... })
})
```
**Result:** `403 Access Denied`

**Fix:** Check the role before showing the UI element:
```js
const role = localStorage.getItem('role');
if (role === 'ADMIN') {
  showCreateProductButton();  // Only admins see this
}
```

---

### 401 vs 403 — What's the Difference?

| Status | Name | Meaning | What to do |
|--------|------|---------|-----------|
| **401** | Unauthorized | "I don't know who you are" — no token, expired token, or invalid token | Redirect to login page |
| **403** | Forbidden | "I know who you are, but you don't have permission" — valid token but wrong role | Show "Access Denied" message, hide restricted UI |

**Frontend error handling pattern:**
```js
const response = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });

if (response.status === 401) {
  // Clear stored auth data and redirect to login
  localStorage.clear();
  window.location.href = '/login';
}

if (response.status === 403) {
  // Show access denied message
  alert('You do not have permission to perform this action.');
}
```

---

### How to Test Role-Based Access

**Create two accounts:**

1. **ADMIN account:**
```bash
POST /api/auth/register
{
  "emailAddress": "admin@test.com",
  "password": "admin123",
  "role": "ADMIN"
}
```

2. **CUSTOMER account:**
```bash
POST /api/auth/register
{
  "emailAddress": "customer@test.com",
  "password": "customer123",
  "role": "CUSTOMER"
}
```

**Login with each and compare:**
| Endpoint | ADMIN Response | CUSTOMER Response |
|----------|---------------|-------------------|
| `GET /api/orders` | ✅ 200 (all orders) | ❌ 403 Access Denied |
| `GET /api/orders/user/{customerId}` | ✅ 200 (that customer's orders) | ✅ 200 (own orders) |
| `GET /api/users` | ✅ 200 (all users) | ❌ 403 Access Denied |
| `POST /api/products` | ✅ 201 Created | ❌ 403 Access Denied |
| `POST /api/orders` | ✅ 201 Created | ✅ 201 Created |

---

### Debugging Checklist

If you get `403 Access Denied`:

1. ✅ Check the endpoint requires the role you have
2. ✅ Verify you stored the role from the login response: `localStorage.getItem('role')`
3. ✅ Decode your JWT at [jwt.io](https://jwt.io) — check the `role` claim matches
4. ✅ If the endpoint is user-specific (e.g., `/api/orders/user/{userId}`), make sure you're passing **your own userId**, not someone else's

If you get `401 Unauthorized`:

1. ✅ Check you included the `Authorization: Bearer <token>` header
2. ✅ Check the token isn't expired (default: 24 hours) — decode it at [jwt.io](https://jwt.io) and check the `exp` claim
3. ✅ Check you didn't log out — the token may be blacklisted
4. ✅ Check the token wasn't tampered with — if you modified localStorage manually, the signature won't match
