# Why You're Getting 403 Access Denied & OAuth2 Setup

## TL;DR

Your `CUSTOMER` token is working correctly. The 403 errors mean you're calling **ADMIN-only endpoints**. Use the user-specific endpoints instead.

---

## OAuth2 Google Login — How the Redirect Works

### Current Behavior (no `redirect-uri` configured)

You visit `http://localhost:8080/oauth2/authorization/google`, Google authenticates you, and the browser shows raw JSON:

```json
{
  "data": {
    "userId": "1c6627ae-6bff-4916-ae2d-82be59629e2c",
    "email": "adamsmiracle0@gmail.com",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "role": "CUSTOMER"
  },
  "message": "OAuth2 authentication successful",
  "status": true
}
```

This is the **JSON mode** — correct for Postman/API testing. For a real frontend, configure the redirect.

---

### How to Configure the Frontend Redirect

**Step 1 — Set `app.oauth2.redirect-uri` in `application.yaml`:**

```yaml
app:
  oauth2:
    redirect-uri: http://localhost:3000/oauth/callback   # React
    # redirect-uri: http://localhost:5173/oauth/callback  # Vite / Vue
    # redirect-uri: http://localhost:4200/oauth/callback  # Angular
```

Or set the environment variable:
```
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth/callback
```

**Step 2 — After this is set, Google login redirects to:**

```
http://localhost:3000/oauth/callback
  ?token=eyJhbGciOiJIUzI1NiJ9...
  &userId=1c6627ae-6bff-4916-ae2d-82be59629e2c
  &role=CUSTOMER
  &email=adamsmiracle0%40gmail.com
```

**Step 3 — Create the callback page in your frontend:**

```js
// React — pages/OAuthCallback.jsx  (route: /oauth/callback)
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function OAuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);

    const token  = params.get('token');
    const userId = params.get('userId');
    const role   = params.get('role');
    const email  = params.get('email');

    if (token) {
      // Store exactly the same way as regular login
      localStorage.setItem('token',  token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('role',   role);
      localStorage.setItem('email',  email);

      // Redirect to dashboard or home
      navigate(role === 'ADMIN' ? '/admin/dashboard' : '/dashboard');
    } else {
      // No token — something went wrong
      navigate('/login?error=oauth_failed');
    }
  }, []);

  return <p>Signing you in...</p>;
}
```

```js
// Vue 3 — pages/OAuthCallback.vue
<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

onMounted(() => {
  const params = new URLSearchParams(window.location.search);
  const token  = params.get('token');
  const userId = params.get('userId');
  const role   = params.get('role');
  const email  = params.get('email');

  if (token) {
    localStorage.setItem('token',  token);
    localStorage.setItem('userId', userId);
    localStorage.setItem('role',   role);
    localStorage.setItem('email',  email);
    router.push(role === 'ADMIN' ? '/admin' : '/home');
  } else {
    router.push('/login?error=oauth_failed');
  }
});
</script>

<template><p>Signing you in...</p></template>
```

**Step 4 — Add the Google login button:**

```jsx
// Trigger the OAuth2 flow — must be a real browser redirect, NOT fetch/axios
<button onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}>
  Login with Google
</button>
```

**Step 5 — After the callback, use the token exactly like regular login:**

```js
fetch('http://localhost:8080/api/orders/user/' + localStorage.getItem('userId'), {
  headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
})
```

---

## TL;DR

Your `CUSTOMER` token is working correctly. The 403 errors mean you're calling **ADMIN-only endpoints**. Use the user-specific endpoints instead.

---

## The Problem

You logged in as a `CUSTOMER` and got this token:

```json
{
  "userId": "6491e678-6155-42c8-aeca-05f34688b68a",
  "role": "CUSTOMER",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Then your frontend called:
```
GET /api/orders      ← This is ADMIN-only (lists ALL orders from ALL users)
GET /api/users       ← This is ADMIN-only (lists ALL users)
```

The server validated your token successfully, saw `role: CUSTOMER`, checked the `@PreAuthorize("hasRole('ADMIN')")` annotation, and correctly rejected the request with **403 Forbidden**.

---

## The Fix

| What you called (wrong) | What you should call |
|------------------------|---------------------|
| `GET /api/orders` | `GET /api/orders/user/{userId}` |
| `GET /api/users` | `GET /api/users/{userId}` (for your own profile) |
| `GET /api/cart` | `GET /api/cart/user/{userId}` |

---

## Example — Fetch Your Own Orders

**❌ Wrong (this is what caused the 403):**
```js
// This tries to fetch ALL orders from ALL users — ADMIN only
fetch('http://localhost:8080/api/orders', {
  headers: { 'Authorization': `Bearer ${token}` }
})
// → 403 Access Denied
```

**✅ Correct:**
```js
// Fetch only YOUR orders
const userId = localStorage.getItem('userId');  // from login response
fetch(`http://localhost:8080/api/orders/user/${userId}`, {
  headers: { 'Authorization': `Bearer ${token}` }
})
// → 200 OK with your orders
```

---

## The Right Pattern for Every Resource

```js
// After login, store userId
const { token, role, userId } = loginResponse.data.data;
localStorage.setItem('token', token);
localStorage.setItem('role', role);
localStorage.setItem('userId', userId);

// Then use it in all user-specific endpoints
const userId = localStorage.getItem('userId');

// ✅ My cart
fetch(`/api/cart/user/${userId}`)

// ✅ My orders
fetch(`/api/orders/user/${userId}`)

// ✅ My profile
fetch(`/api/users/${userId}`)

// ✅ My addresses
fetch(`/api/addresses/user/${userId}`)

// ✅ My payment methods
fetch(`/api/payment-methods/user/${userId}`)

// ✅ My reviews
fetch(`/api/reviews/user/${userId}`)
```

---

## When to Show Admin-Only UI

```js
const role = localStorage.getItem('role');
const isAdmin = role === 'ADMIN';
const isCustomer = role === 'CUSTOMER';

// Conditionally render based on role
{isAdmin && <Link to="/admin/users">Manage Users</Link>}
{isAdmin && <Link to="/admin/orders">All Orders</Link>}
{isAdmin && <button onClick={createProduct}>Create Product</button>}

{isCustomer && <Link to="/my-orders">My Orders</Link>}
{isCustomer && <Link to="/cart">My Cart</Link>}
```

---

## How to Interpret the Logs

```
JWT_VALIDATION_SUCCESS — Role: CUSTOMER
                    ↓
Failed to authorize ... hasRole('ADMIN')
                    ↓
ACCESS_DENIED /api/orders
```

This means:
1. ✅ Your token is **valid** — authentication worked
2. ❌ Your role (`CUSTOMER`) doesn't match the required role (`ADMIN`) — authorization failed
3. ✅ The server is working correctly — it rejected an unauthorized request

---

## Quick Endpoint Cheat Sheet

| I want to... | Endpoint | Role Required |
|-------------|----------|---------------|
| See my cart | `GET /api/cart/user/{userId}` | CUSTOMER, ADMIN |
| See my orders | `GET /api/orders/user/{userId}` | CUSTOMER, ADMIN |
| See my profile | `GET /api/users/{userId}` | CUSTOMER, ADMIN |
| Place an order | `POST /api/orders` | CUSTOMER, ADMIN |
| Browse products | `GET /api/products` | Everyone (no token needed) |
| Create a product | `POST /api/products` | ADMIN only |
| See all orders (from everyone) | `GET /api/orders` | ADMIN only |
| See all users | `GET /api/users` | ADMIN only |
| See all carts | `GET /api/cart` | ADMIN only |

---

## Still Getting 403?

1. Open your browser console
2. Run this:
```js
const token = localStorage.getItem('token');
const parts = token.split('.');
const payload = JSON.parse(atob(parts[1]));
console.log('My role:', payload.role);
console.log('My userId:', payload.sub);
```
3. Check if the role matches what the endpoint requires (see API_REFERENCE.md)
4. If the role is correct but still getting 403, verify you're using the user-specific endpoint (`/user/{userId}`) not the admin-only one

