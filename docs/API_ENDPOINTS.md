# API Endpoints Documentation

## Overview

This document lists all the REST API endpoints available in the Smart E-Commerce JPA application, including their request and response data structures.

## Base URL

```
http://localhost:8080
```

## Standard Response Format

All endpoints return responses in the standardized `ApiResponse<T>` format:

```json
{
  "status": true,
  "message": "Request successful",
  "data": {},
  "timestamp": "2024-01-01T00:00:00Z",
  "statusCode": 200,
  "path": "/api/endpoint",
  "errors": []
}
```

### ApiResponse Fields
- `status`: Boolean indicating success/failure
- `message`: Response message
- `data`: The actual response data (varies by endpoint)
- `timestamp`: Response timestamp
- `statusCode`: HTTP status code
- `path`: Request path (optional)
- `errors`: Field validation errors (optional)

### Paginated Response Format

Endpoints that return paginated data use the `PageResponse<T>` format:

```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 100,
  "totalPages": 10,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## Authentication Endpoints

### POST /api/auth/login
Authenticate user with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "data": {
    "userId": "uuid",
    "role": "CUSTOMER | ADMIN",
    "X-Auth-toke": "token"
  }
}
```

**Status Codes:** 200 (success), 401 (invalid credentials)

---

### POST /api/auth/register
Register a new user account.

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "emailAddress": "john@example.com",
  "password": "password123",
  "phoneNumber": "+1234567890"
}
```

**Response:**
```json
{
  "data": {
    "userId": "uuid",
    "role": "CUSTOMER",
    "token": "jwt-token"
  }
}
```

**Status Codes:** 201 (created), 400 (invalid input)

---

## Home & Health Endpoints

### GET /
Returns welcome message and API information.

**Response:**
```json
{
  "data": {
    "name": "Smart E-Commerce API",
    "version": "1.0.0",
    "description": "A production-ready e-commerce REST API using raw JDBC",
    "timestamp": "2024-01-01T00:00:00Z",
    "endpoints": {
      "users": "/api/users",
      "products": "/api/products",
      "categories": "/api/categories",
      "cart": "/api/cart",
      "orders": "/api/orders",
      "graphql": "/graphql",
      "swagger-ui": "/swagger-ui.html",
      "api-docs": "/v3/api-docs"
    }
  }
}
```

---

### GET /health
Returns API health status.

**Response:**
```json
{
  "data": {
    "status": "UP",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

---

## User Management Endpoints

### POST /api/users
Create a new user account (Admin/Customer).

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "emailAddress": "john@example.com",
  "password": "password123",
  "phoneNumber": "+1234567890"
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "emailAddress": "john@example.com",
    "phoneNumber": "+1234567890",
    "isActive": true,
    "role": "CUSTOMER",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 201 (created), 400 (invalid input), 409 (email exists)

---

### GET /api/users/{id}
Get user by ID (Admin/Customer).

**Response:** Same as user creation response

**Status Codes:** 200 (success), 404 (not found)

---

### GET /api/users/email/{email}
Get user by email address (Admin/Customer).

**Response:** Same as user creation response

---

### GET /api/users
Get all users with pagination (Admin only).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `sort`: Sort field and direction (default: createdAt,desc)

**Response:**
```json
{
  "data": {
    "content": [UserResponse],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### GET /api/users/search
Search users by keyword (name or email) (Admin only).

**Query Parameters:**
- `keyword`: Search term
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated user list

---

### PUT /api/users/{id}
Update user information (Admin only).

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```

**Response:** Updated user data

---

### DELETE /api/users/{id}
Delete user account (Admin only).

**Response:** Success message

---

### POST /api/users/{id}/activate
Activate user account (Admin only).

**Response:** Success message

---

### POST /api/users/{id}/deactivate
Deactivate user account (Admin only).

**Response:** Success message

---

## Address Management Endpoints

### POST /api/addresses
Create a new shipping or billing address.

**Request:**
```json
{
  "userId": "uuid",
  "addressLine1": "123 Main St",
  "addressLine2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "postalCode": "10001",
  "country": "USA",
  "addressType": "shipping",
  "isDefault": true
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "addressLine1": "123 Main St",
    "addressLine2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA",
    "addressType": "shipping",
    "isDefault": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 201 (created), 400 (invalid input), 404 (user not found)

---

### GET /api/addresses/{id}
Get address by ID.

**Response:** Address data

---

### GET /api/addresses
Get all addresses in the system (Admin) - paginated.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated address list

---

### GET /api/addresses/user/{userId}
Get all addresses for a specific user - paginated.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated address list

---

### GET /api/addresses/user/{userId}/shipping
Get all shipping addresses for a user - paginated.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated shipping address list

---

### GET /api/addresses/user/{userId}/billing
Get all billing addresses for a user - paginated.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated billing address list

---

### PUT /api/addresses/{id}
Update an existing address.

**Request:** Same as address creation

**Response:** Updated address data

---

### DELETE /api/addresses/{id}
Delete an address by ID.

**Response:** Success message

---

## Category Management Endpoints

### POST /api/categories
Create a new product category (Admin only).

**Request:**
```json
{
  "name": "Electronics",
  "description": "Electronic devices and accessories"
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "name": "Electronics",
    "description": "Electronic devices and accessories",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 201 (created)

---

### GET /api/categories/{id}
Get category by ID.

**Response:** Category data

---

### GET /api/categories
Get all categories (paginated).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated category list

---

### PUT /api/categories/{id}
Update an existing category (Admin only).

**Request:** Same as category creation

**Response:** Updated category data

---

### DELETE /api/categories/{id}
Delete a category by ID (Admin only).

**Response:** Success message

---

## Product Management Endpoints

### POST /api/products
Create a new product (Admin only).

**Request:**
```json
{
  "name": "Smartphone",
  "description": "Latest smartphone with advanced features",
  "price": 999.99,
  "categoryId": "uuid",
  "sku": "PHONE-001",
  "stockQuantity": 100,
  "imageUrl": "https://example.com/image.jpg",
  "isActive": true
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "name": "Smartphone",
    "description": "Latest smartphone with advanced features",
    "price": 999.99,
    "categoryId": "uuid",
    "categoryName": "Electronics",
    "sku": "PHONE-001",
    "stockQuantity": 100,
    "imageUrl": "https://example.com/image.jpg",
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 201 (created), 400 (invalid input), 404 (category not found)

---

### GET /api/products/{id}
Get product by ID.

**Response:** Product data

---

### GET /api/products
Get all products with pagination.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated product list

---

### GET /api/products/active
Get active products with pagination.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated active product list

---

### GET /api/products/category/{categoryId}
Get products by category ID.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated product list for category

---

### GET /api/products/search
Search products by keyword.

**Query Parameters:**
- `keyword`: Search term
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated search results

---

### GET /api/products/price-range
Get products within a price range.

**Query Parameters:**
- `minPrice`: Minimum price
- `maxPrice`: Maximum price
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated product list

---

### GET /api/products/in-stock
Get products that are in stock.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated in-stock product list

---

### PUT /api/products/{id}
Update an existing product (Admin only).

**Request:**
```json
{
  "name": "Updated Smartphone",
  "description": "Updated description",
  "price": 899.99,
  "sku": "PHONE-001-UPDATED",
  "imageUrl": "https://example.com/new-image.jpg"
}
```

**Response:** Updated product data

---

### DELETE /api/products/{id}
Delete a product by ID (Admin only).

**Response:** Success message

---

### POST /api/products/{id}/activate
Activate a product (Admin only).

**Response:** Success message

---

### POST /api/products/{id}/deactivate
Deactivate a product (Admin only).

**Response:** Success message

---

### PATCH /api/products/{id}/stock
Update product stock quantity (Admin only).

**Query Parameters:**
- `quantity`: Quantity to add (negative to reduce)

**Response:** Success message

---

## Shopping Cart Endpoints

### GET /api/cart
Get all shopping carts with pagination (Admin).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated cart list

---

### GET /api/cart/user/{userId}
Get user's shopping cart.

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "items": [
      {
        "id": "uuid",
        "productId": "uuid",
        "productName": "Smartphone",
        "productPrice": 999.99,
        "quantity": 2,
        "subtotal": 1999.98
      }
    ],
    "totalAmount": 1999.98,
    "itemCount": 2,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### POST /api/cart/user/{userId}/items
Add item to shopping cart.

**Request:**
```json
{
  "productId": "uuid",
  "quantity": 2
}
```

**Response:** Updated cart data

---

### PUT /api/cart/user/{userId}/items/{itemId}
Update cart item quantity.

**Query Parameters:**
- `quantity`: New quantity

**Response:** Updated cart data

---

### DELETE /api/cart/user/{userId}/items/{itemId}
Remove item from shopping cart.

**Response:** Updated cart data

---

### DELETE /api/cart/user/{userId}
Clear all items from shopping cart.

**Response:** Success message

---

### GET /api/cart/user/{userId}/count
Get total number of items in cart.

**Response:**
```json
{
  "data": 5
}
```

---

### GET /api/cart/count
Get cart item count (query parameter).

**Query Parameters:**
- `userId`: User ID

**Response:** Item count

---

## Order Management Endpoints

### POST /api/orders
Create a new order (checkout).

**Request:**
```json
{
  "userId": "uuid",
  "shippingAddressId": "uuid",
  "billingAddressId": "uuid",
  "paymentMethodId": "uuid",
  "shippingMethodId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2,
      "price": 999.99
    }
  ],
  "notes": "Special delivery instructions"
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "orderNumber": "ORD-2024-001",
    "userId": "uuid",
    "status": "pending",
    "paymentStatus": "pending",
    "totalAmount": 1999.98,
    "shippingCost": 10.00,
    "taxAmount": 200.00,
    "finalAmount": 2209.98,
    "items": [
      {
        "id": "uuid",
        "productId": "uuid",
        "productName": "Smartphone",
        "quantity": 2,
        "unitPrice": 999.99,
        "totalPrice": 1999.98
      }
    ],
    "shippingAddress": AddressResponse,
    "billingAddress": AddressResponse,
    "paymentMethod": PaymentMethodResponse,
    "shippingMethod": ShippingMethodResponse,
    "notes": "Special delivery instructions",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 201 (created), 400 (invalid input), 404 (user/product not found)

---

### GET /api/orders/{id}
Get order by ID.

**Response:** Order data

**Status Codes:** 200 (success), 400 (invalid UUID), 404 (not found)

---

### GET /api/orders/number/{orderNumber}
Get order by order number.

**Response:** Order data

---

### GET /api/orders
Get all orders with pagination (Admin only).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated order list

---

### GET /api/orders/user/{userId}
Get orders for a specific user.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated order list

---

### GET /api/orders/status/{status}
Get orders filtered by status (Admin only).

**Path Parameters:**
- `status`: Order status (pending, confirmed, processing, shipped, delivered, cancelled)

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated order list

---

### PATCH /api/orders/{id}/status
Update order status (Admin only).

**Query Parameters:**
- `status`: New status

**Response:** Updated order data

---

### PATCH /api/orders/{id}/payment-status
Update payment status (Admin only).

**Query Parameters:**
- `paymentStatus`: Payment status (pending, paid, failed, refunded)

**Response:** Updated order data

---

### POST /api/orders/{id}/cancel
Cancel an order.

**Response:** Updated order data

**Status Codes:** 200 (success), 400 (cannot cancel), 404 (not found)

---

### DELETE /api/orders/{id}
Delete an order (Admin only).

**Response:** Success message

---

### GET /api/orders/count
Get total order count (Admin only).

**Response:**
```json
{
  "data": 150
}
```

---

### GET /api/orders/count/status/{status}
Get order count by status (Admin only).

**Response:** Order count for status

---

### PUT /api/orders/{id}
Update editable order fields (Admin only).

**Request:**
```json
{
  "notes": "Updated notes",
  "shippingAddressId": "uuid",
  "billingAddressId": "uuid"
}
```

**Response:** Updated order data

---

## Payment Method Endpoints

### POST /api/payment-methods
Create a new payment method for a user.

**Request:**
```json
{
  "userId": "uuid",
  "methodType": "credit_card",
  "cardNumber": "4111111111111111",
  "cardholderName": "John Doe",
  "expiryDate": "12/25",
  "cvv": "123",
  "isDefault": true
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "methodType": "credit_card",
    "cardNumberMasked": "****-****-****-1111",
    "cardholderName": "John Doe",
    "expiryDate": "12/25",
    "isDefault": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 200 (success), 400 (invalid input)

---

### PUT /api/payment-methods/{id}
Update an existing payment method.

**Request:** Same as creation (excluding userId)

**Response:** Updated payment method data

**Status Codes:** 200 (success), 404 (not found)

---

### GET /api/payment-methods/{id}
Get payment method by ID.

**Response:** Payment method data

---

### GET /api/payment-methods/user/{userId}
List payment methods for a specific user (paginated).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated payment method list

---

### DELETE /api/payment-methods/{id}
Delete a payment method by ID.

**Response:** Success message

**Status Codes:** 204 (success), 404 (not found)

---

## Shipping Method Endpoints

### POST /api/shipping-methods
Create a new shipping method (Admin only).

**Request:**
```json
{
  "name": "Standard Shipping",
  "description": "Standard delivery within 5-7 business days",
  "cost": 10.00,
  "estimatedDays": 5,
  "isActive": true
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "name": "Standard Shipping",
    "description": "Standard delivery within 5-7 business days",
    "cost": 10.00,
    "estimatedDays": 5,
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 200 (success), 400 (invalid input)

---

### PUT /api/shipping-methods/{id}
Update an existing shipping method (Admin only).

**Request:** Same as creation

**Response:** Updated shipping method data

**Status Codes:** 200 (success), 404 (not found)

---

### GET /api/shipping-methods/{id}
Get shipping method by ID.

**Response:** Shipping method data

---

### GET /api/shipping-methods
List all shipping methods (paginated).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated shipping method list

---

### DELETE /api/shipping-methods/{id}
Delete a shipping method by ID (Admin only).

**Response:** Success message

**Status Codes:** 204 (success), 404 (not found)

---

## Product Review Endpoints

### POST /api/reviews
Create a new product review.

**Request:**
```json
{
  "userId": "uuid",
  "productId": "uuid",
  "rating": 5,
  "title": "Great product!",
  "comment": "Excellent quality and fast shipping."
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "productId": "uuid",
    "productName": "Smartphone",
    "rating": 5,
    "title": "Great product!",
    "comment": "Excellent quality and fast shipping.",
    "isVerified": true,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Status Codes:** 201 (created), 400 (invalid input or already reviewed), 404 (not found)

---

### GET /api/reviews
Get all reviews (paginated).

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated review list

---

### GET /api/reviews/{id}
Get review by ID.

**Response:** Review data

---

### GET /api/reviews/product/{productId}
Get reviews for a specific product.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated review list

---

### GET /api/reviews/user/{userId}
Get reviews submitted by a specific user.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:** Paginated review list

---

### GET /api/reviews/product/{productId}/average-rating
Get average rating for a product.

**Response:**
```json
{
  "data": 4.5
}
```

---

### GET /api/reviews/product/{productId}/count
Get number of reviews for a product.

**Response:**
```json
{
  "data": 25
}
```

---

### GET /api/reviews/check
Check if user has already reviewed a product.

**Query Parameters:**
- `userId`: User ID
- `productId`: Product ID

**Response:**
```json
{
  "data": true
}
```

---

### PUT /api/reviews/{id}
Update an existing review.

**Request:** Same as review creation

**Response:** Updated review data

---

### DELETE /api/reviews/{id}
Delete a review by ID.

**Response:** Success message

---

## Cache Management Endpoints

### GET /api/cache/statistics
Get cache statistics and performance metrics.

**Response:**
```json
{
  "data": {
    "productCache": {
      "hitCount": 1000,
      "missCount": 100,
      "hitRate": 0.909,
      "size": 50,
      "maxSize": 100
    },
    "userCache": {
      "hitCount": 500,
      "missCount": 50,
      "hitRate": 0.909,
      "size": 25,
      "maxSize": 50
    }
  }
}
```

---

### GET /api/cache/health
Check cache health status.

**Response:**
```json
{
  "data": {
    "overallHealth": "HEALTHY",
    "caches": {
      "productCache": "HEALTHY",
      "userCache": "HEALTHY",
      "categoryCache": "HEALTHY",
      "orderCache": "HEALTHY"
    }
  }
}
```

---

### GET /api/cache/recommendations
Get performance recommendations based on cache usage.

**Response:**
```json
{
  "data": {
    "recommendations": [
      "Consider increasing product cache size",
      "User cache is performing optimally",
      "Category cache hit rate could be improved"
    ],
    "priority": "MEDIUM"
  }
}
```

---

### POST /api/cache/warmup/products
Warm up product cache with frequently accessed products.

**Response:**
```json
{
  "data": "Product cache warm-up initiated",
  "message": "Cache warm-up started successfully"
}
```

---

### POST /api/cache/warmup/users
Warm up user cache with active user profiles.

**Response:** Success message

---

### POST /api/cache/warmup/categories
Warm up category cache with product counts.

**Response:** Success message

---

### DELETE /api/cache/products
Clear all entries from product cache.

**Response:** Success message

---

### DELETE /api/cache/users
Clear all entries from user cache.

**Response:** Success message

---

### DELETE /api/cache/categories
Clear all entries from category cache.

**Response:** Success message

---

### DELETE /api/cache/orders
Clear all entries from order cache.

**Response:** Success message

---

### DELETE /api/cache/all
Clear all caches in the application.

**Response:** Success message

---

### GET /api/cache/performance
Get comprehensive performance report including statistics, health, and recommendations.

**Response:**
```json
{
  "data": {
    "statistics": {},
    "health": {},
    "recommendations": {},
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

---

## Authentication & Authorization

### Role-Based Access Control

The API uses role-based access control with the following roles:

- **ADMIN**: Full access to all endpoints
- **CUSTOMER**: Limited access to customer-specific endpoints

### JWT Authentication

Most endpoints require JWT authentication via the `Authorization` header:

```
Authorization: Bearer <jwt-token>
```

### Public Endpoints

The following endpoints do not require authentication:
- `GET /`
- `GET /health`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/products` (and product-related GET endpoints)
- `GET /api/categories` (and category-related GET endpoints)

---

## Error Handling

### Standard Error Response

```json
{
  "status": false,
  "message": "Error description",
  "statusCode": 400,
  "timestamp": "2024-01-01T00:00:00Z",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

### Common HTTP Status Codes

- **200 OK**: Request successful
- **201 Created**: Resource created successfully
- **204 No Content**: Resource deleted successfully
- **400 Bad Request**: Invalid input data
- **401 Unauthorized**: Authentication required/failed
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **409 Conflict**: Resource conflict (e.g., duplicate email)
- **500 Internal Server Error**: Server error

---

## Pagination

### Standard Pagination Parameters

All paginated endpoints support the following query parameters:

- `page`: Page number (0-based, default: 0)
- `size`: Page size (default: 10)
- `sort`: Sort field and direction (format: field,direction)

### Example

```
GET /api/products?page=0&size=20&sort=price,asc
```

---

## Search and Filtering

### Search Endpoints

Several endpoints support search functionality:

- `GET /api/products/search?keyword=phone`
- `GET /api/users/search?keyword=john`

### Filtering

Some endpoints support filtering:

- `GET /api/products/price-range?minPrice=100&maxPrice=1000`
- `GET /api/products/in-stock`
- `GET /api/products/active`
- `GET /api/orders/status/pending`

---

## Data Validation

All request bodies are validated using Jakarta Bean Validation annotations. Common validation rules include:

- `@NotNull`: Field must not be null
- `@Email`: Valid email format
- `@Size(min/max)`: String length constraints
- `@Min/@Max`: Numeric value constraints
- `@Valid`: Nested object validation

---

## Rate Limiting

The API implements rate limiting to prevent abuse. Default limits:

- **Anonymous users**: 100 requests per hour
- **Authenticated users**: 1000 requests per hour
- **Admin users**: 5000 requests per hour

Rate limit headers are included in responses:

```
X-Rate-Limit-Remaining: 950
X-Rate-Limit-Reset: 1640995200
```

---

## GraphQL Endpoint

### POST /graphql

GraphQL endpoint for flexible queries and mutations. Refer to the GraphQL schema documentation for available operations.

---

## Swagger Documentation

Interactive API documentation is available at:

- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI JSON**: `/v3/api-docs`

---

## WebSocket Support

Real-time notifications are supported via WebSocket connections:

- **WebSocket URL**: `ws://localhost:8080/ws`

Supported events:
- Order status updates
- Stock level changes
- New product notifications

---

## File Upload

### POST /api/upload

Upload product images and other files.

**Request:** `multipart/form-data`
- `file`: File to upload
- `type`: File type (image, document, etc.)

**Response:**
```json
{
  "data": {
    "url": "https://example.com/uploads/image.jpg",
    "filename": "image.jpg",
    "size": 1024000,
    "contentType": "image/jpeg"
  }
}
```

---

## Bulk Operations

### POST /api/products/bulk
Bulk create products (Admin only).

### POST /api/users/bulk
Bulk create users (Admin only).

### DELETE /api/products/bulk
Bulk delete products (Admin only).

---

## Export/Import

### GET /api/export/products
Export products to CSV/Excel (Admin only).

### POST /api/import/products
Import products from CSV/Excel (Admin only).

---

## Analytics & Reporting

### GET /api/analytics/sales
Get sales analytics and reports (Admin only).

### GET /api/analytics/products
Get product performance analytics (Admin only).

### GET /api/analytics/users
Get user behavior analytics (Admin only).

---

## Health Checks

### GET /api/health/database
Database connectivity check.

### GET /api/health/cache
Cache system health check.

### GET /api/health/external
External service connectivity check.

---

## Configuration

### GET /api/config
Get application configuration (Admin only).

### PUT /api/config
Update application configuration (Admin only).

---

## Logging

### GET /api/logs
Get application logs (Admin only).

### GET /api/logs/level
Get current logging levels.

### PUT /api/logs/level
Update logging levels (Admin only).

---

## Security

### GET /api/security/audit
Get security audit log (Admin only).

### POST /api/security/lock/{userId}
Lock user account (Admin only).

### POST /api/security/unlock/{userId}
Unlock user account (Admin only).

---

## Notifications

### GET /api/notifications
Get user notifications.

### POST /api/notifications/mark-read/{id}
Mark notification as read.

### DELETE /api/notifications/{id}
Delete notification.

---

## Webhooks

### POST /api/webhooks
Create webhook endpoint (Admin only).

### GET /api/webhooks
List webhook endpoints (Admin only).

### PUT /api/webhooks/{id}
Update webhook endpoint (Admin only).

### DELETE /api/webhooks/{id}
Delete webhook endpoint (Admin only).

---

## API Versioning

The API supports versioning through URL paths:

- Current version: `/api/v1/`
- Legacy version: `/api/v1/` (same as current)

Future versions will be available at `/api/v2/`, etc.

---

## Testing

### Test Endpoints

- `GET /api/test/ping`: Simple ping test
- `GET /api/test/auth`: Authentication test
- `GET /api/test/database`: Database connectivity test

These endpoints are only available in test/development environments.

---

## Documentation Version

This documentation corresponds to API version 1.0.0.

For the most up-to-date information, always refer to the live Swagger documentation at `/swagger-ui.html`.
