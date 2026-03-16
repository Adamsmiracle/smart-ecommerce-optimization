# Smart E-Commerce Security — Database Reference

---

## Part 1 — Full Database Schema

11 tables, added by **V1__Initial_schema.sql** and **V2__Add_oauth_provider_columns.sql**.

> **Legend**  
> 🔑 Primary Key &nbsp;|&nbsp; 🔗 Foreign Key &nbsp;|&nbsp; 🔒 Unique &nbsp;|&nbsp; ✳️ Not Null &nbsp;|&nbsp; ☑️ Has Default

---

### 1. `app_user`

> Stores every account — admin, staff, and customer, including OAuth2 users (Google).  
> Modified by **V2** to make `password_hash` nullable and add `oauth_provider` / `oauth_provider_id`.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | Auto-generated primary key |
| 2 | `email_address` | `VARCHAR(255)` | 🔒 ✳️ | — | Login identifier; indexed |
| 3 | `first_name` | `VARCHAR(100)` | — | `NULL` | Optional |
| 4 | `last_name` | `VARCHAR(100)` | — | `NULL` | Optional |
| 5 | `phone_number` | `VARCHAR(20)` | — | `NULL` | Optional |
| 6 | `password_hash` | `VARCHAR(255)` | — | `NULL` | BCrypt hash; **NULL for OAuth2-only users** (added by V2) |
| 7 | `is_active` | `BOOLEAN` | ✳️ | `TRUE` | Soft-disable accounts |
| 8 | `role` | `VARCHAR(50)` | ✳️ | `'CUSTOMER'` | Values: `ADMIN`, `STAFF`, `CUSTOMER` |
| 9 | `oauth_provider` | `VARCHAR(50)` | — | `NULL` | e.g. `'google'` (added by V2) |
| 10 | `oauth_provider_id` | `VARCHAR(255)` | — | `NULL` | Google `sub` claim (added by V2) |
| 11 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 12 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | Set by JPA `@PreUpdate` |

**Indexes**

| Index | Columns | Type |
|---|---|---|
| `idx_user_email` | `email_address` | B-tree |
| `idx_user_role` | `role` | B-tree |
| `idx_user_email_active` | `(email_address, is_active)` | Composite |
| `idx_user_oauth` | `(oauth_provider, oauth_provider_id)` | Composite (V2) |

**Referenced by:** `user_address`, `payment_method`, `shopping_cart`, `customer_order`, `product_review`

---

### 2. `user_address`

> Shipping and billing addresses belonging to a user.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `user_id` | `UUID` | 🔗 ✳️ | — | → `app_user(id)` ON DELETE CASCADE |
| 3 | `address_line` | `VARCHAR(255)` | ✳️ | — | Street / building |
| 4 | `city` | `VARCHAR(100)` | ✳️ | — | — |
| 5 | `region` | `VARCHAR(100)` | — | `NULL` | State / province |
| 6 | `country` | `VARCHAR(100)` | ✳️ | — | — |
| 7 | `postal_code` | `VARCHAR(20)` | — | `NULL` | — |
| 8 | `address_type` | `VARCHAR(50)` | — | `NULL` | e.g. `SHIPPING`, `BILLING` |
| 9 | `is_default` | `BOOLEAN` | ✳️ | `FALSE` | One default per type per user |
| 10 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 11 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns | Type |
|---|---|---|
| `idx_user_address_user_id` | `user_id` | B-tree |
| `idx_address_user` | `user_id` | B-tree (duplicate — legacy) |

---

### 3. `product_category`

> Taxonomy for products. Each product belongs to exactly one category.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `category_name` | `VARCHAR(100)` | 🔒 ✳️ | — | Must be unique |
| 3 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 4 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Referenced by:** `product`

---

### 4. `product`

> The product catalogue. `images` is a JSON array stored as TEXT.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `category_id` | `UUID` | 🔗 ✳️ | — | → `product_category(id)` ON DELETE RESTRICT |
| 3 | `name` | `VARCHAR(255)` | ✳️ | — | — |
| 4 | `description` | `VARCHAR(2000)` | — | `NULL` | — |
| 5 | `price` | `DECIMAL(19,2)` | ✳️ CHECK ≥ 0 | — | — |
| 6 | `stock_quantity` | `INTEGER` | ✳️ CHECK ≥ 0 | `0` | — |
| 7 | `is_active` | `BOOLEAN` | ✳️ | `TRUE` | Soft-remove products |
| 8 | `images` | `TEXT` | — | `NULL` | JSON array: `["img1.jpg","img2.jpg"]` |
| 9 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 10 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns | Type | Notes |
|---|---|---|---|
| `idx_product_category_id` | `category_id` | B-tree | — |
| `idx_product_is_active` | `is_active` | B-tree | — |
| `idx_product_category_active` | `(category_id, is_active)` | Composite | Active-products-by-category query |
| `idx_product_price_range` | `price` | B-tree | Price-range filter |
| `idx_product_name_active` | `(LOWER(name), is_active)` WHERE `is_active=TRUE` | Partial | Case-insensitive search |

**Referenced by:** `cart_item`, `order_item`, `product_review`

---

### 5. `shopping_cart`

> One cart per user. Acts as the parent record for `cart_item`.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `user_id` | `UUID` | 🔗 ✳️ | — | → `app_user(id)` ON DELETE CASCADE |
| 3 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 4 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns |
|---|---|
| `idx_shopping_cart_user_id` | `user_id` |

**Referenced by:** `cart_item`

---

### 6. `cart_item`

> Individual line items inside a cart. A product can appear in a cart at most once — enforced by `UNIQUE(cart_id, product_id)`.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `cart_id` | `UUID` | 🔗 ✳️ | — | → `shopping_cart(id)` ON DELETE CASCADE |
| 3 | `product_id` | `UUID` | 🔗 ✳️ | — | → `product(id)` ON DELETE CASCADE |
| 4 | `quantity` | `INTEGER` | ✳️ CHECK > 0 | `1` | Must be at least 1 |
| 5 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 6 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Table constraints:** `UNIQUE(cart_id, product_id)` — prevents duplicate product entries per cart

**Indexes**

| Index | Columns |
|---|---|
| `idx_cart_item_cart_id` | `cart_id` |
| `idx_cart_item_product_id` | `product_id` |

---

### 7. `payment_method`

> Saved payment instruments for a user. Soft-deleted via `is_active`.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `user_id` | `UUID` | 🔗 ✳️ | — | → `app_user(id)` ON DELETE CASCADE |
| 3 | `payment_type` | `VARCHAR(50)` | ✳️ | — | e.g. `CREDIT_CARD`, `PAYPAL` |
| 4 | `provider` | `VARCHAR(100)` | — | `NULL` | e.g. `Visa`, `Mastercard`, `PayPal` |
| 5 | `account_number` | `VARCHAR(255)` | ✳️ | — | Masked; e.g. `****-****-****-4242` |
| 6 | `expiry_date` | `TIMESTAMPTZ` | ✳️ | — | Card or account expiry |
| 7 | `is_default` | `BOOLEAN` | ✳️ | `FALSE` | One default per user |
| 8 | `is_active` | `BOOLEAN` | ✳️ | `TRUE` | Soft-delete |
| 9 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 10 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns |
|---|---|
| `idx_payment_method_user_id` | `user_id` |
| `idx_payment_method_is_active` | `is_active` |

**Referenced by:** `customer_order`

---

### 8. `shipping_method`

> Catalogue of available shipping options. Soft-deleted via `is_active`.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `name` | `VARCHAR(100)` | ✳️ | — | e.g. `Standard Shipping` |
| 3 | `description` | `VARCHAR(500)` | — | `NULL` | Customer-facing description |
| 4 | `price` | `DECIMAL(19,2)` | ✳️ CHECK ≥ 0 | — | Shipping fee |
| 5 | `estimated_days` | `INTEGER` | CHECK ≥ 0 | `NULL` | Expected delivery window |
| 6 | `is_active` | `BOOLEAN` | ✳️ | `TRUE` | Hide without deleting |
| 7 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 8 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns |
|---|---|
| `idx_shipping_method_is_active` | `is_active` |

**Referenced by:** `customer_order`

---

### 9. `customer_order`

> The order header. Holds totals, status, and references to the payment and shipping methods chosen at checkout.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `user_id` | `UUID` | 🔗 ✳️ | — | → `app_user(id)` ON DELETE RESTRICT |
| 3 | `order_number` | `VARCHAR(50)` | 🔒 ✳️ | — | Human-readable; e.g. `ORD-2026-000001` |
| 4 | `status` | `VARCHAR(30)` | ✳️ | `'pending'` | `pending` → `confirmed` → `processing` → `shipped` → `delivered` / `cancelled` |
| 5 | `payment_method_id` | `UUID` | 🔗 | `NULL` | → `payment_method(id)` |
| 6 | `shipping_method_id` | `UUID` | 🔗 | `NULL` | → `shipping_method(id)` |
| 7 | `subtotal` | `DECIMAL(19,2)` | ✳️ CHECK ≥ 0 | — | Sum of line items |
| 8 | `total` | `DECIMAL(19,2)` | ✳️ CHECK ≥ 0 | — | subtotal + shipping |
| 9 | `payment_status` | `VARCHAR(30)` | — | `'pending'` | `pending`, `paid`, `failed`, `refunded` |
| 10 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 11 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns | Type |
|---|---|---|
| `idx_customer_order_user_id` | `user_id` | B-tree |
| `idx_customer_order_status` | `status` | B-tree |
| `idx_customer_order_order_number` | `order_number` | B-tree |
| `idx_customer_order_payment_status` | `payment_status` | B-tree |
| `idx_customer_order_user_status` | `(user_id, status)` | Composite |
| `idx_customer_order_created_at` | `created_at DESC` | Descending |

**Referenced by:** `order_item`

---

### 10. `order_item`

> Individual line items for an order. Stores `unit_price` at time of purchase (snapshot — price changes don't affect old orders).

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `order_id` | `UUID` | 🔗 ✳️ | — | → `customer_order(id)` ON DELETE CASCADE |
| 3 | `product_id` | `UUID` | 🔗 ✳️ | — | → `product(id)` ON DELETE RESTRICT |
| 4 | `unit_price` | `DECIMAL(19,2)` | ✳️ CHECK ≥ 0 | — | Price at time of order |
| 5 | `quantity` | `INTEGER` | ✳️ CHECK > 0 | — | — |
| 6 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 7 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns |
|---|---|
| `idx_order_item_order_id` | `order_id` |
| `idx_order_item_product_id` | `product_id` |

---

### 11. `product_review`

> One review per user per product — enforced by `UNIQUE INDEX idx_product_review_user_product`.

| # | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| 1 | `id` | `UUID` | 🔑 ✳️ | `gen_random_uuid()` | — |
| 2 | `user_id` | `UUID` | 🔗 ✳️ | — | → `app_user(id)` ON DELETE CASCADE |
| 3 | `product_id` | `UUID` | 🔗 ✳️ | — | → `product(id)` ON DELETE CASCADE |
| 4 | `rating` | `INTEGER` | ✳️ CHECK 1–5 | — | Star rating 1 to 5 |
| 5 | `comment` | `VARCHAR(2000)` | — | `NULL` | Optional review text |
| 6 | `created_at` | `TIMESTAMPTZ` | ✳️ | `NOW()` | — |
| 7 | `updated_at` | `TIMESTAMPTZ` | — | `NULL` | — |

**Indexes**

| Index | Columns | Type |
|---|---|---|
| `idx_product_review_user_id` | `user_id` | B-tree |
| `idx_product_review_product_id` | `product_id` | B-tree |
| `idx_product_review_rating` | `rating` | B-tree |
| `idx_product_review_user_product` | `(user_id, product_id)` | **Unique** — prevents duplicate reviews |

---

## Foreign Key Overview

| Constraint | Child table → column | Parent table → column | On Delete |
|---|---|---|---|
| `fk_user_address_user` | `user_address.user_id` | `app_user.id` | CASCADE |
| `fk_product_category` | `product.category_id` | `product_category.id` | RESTRICT |
| `fk_shopping_cart_user` | `shopping_cart.user_id` | `app_user.id` | CASCADE |
| `fk_cart_item_cart` | `cart_item.cart_id` | `shopping_cart.id` | CASCADE |
| `fk_cart_item_product` | `cart_item.product_id` | `product.id` | CASCADE |
| `fk_payment_method_user` | `payment_method.user_id` | `app_user.id` | CASCADE |
| `fk_customer_order_user` | `customer_order.user_id` | `app_user.id` | RESTRICT |
| `fk_customer_order_payment_method` | `customer_order.payment_method_id` | `payment_method.id` | — |
| `fk_customer_order_shipping_method` | `customer_order.shipping_method_id` | `shipping_method.id` | — |
| `fk_order_item_order` | `order_item.order_id` | `customer_order.id` | CASCADE |
| `fk_order_item_product` | `order_item.product_id` | `product.id` | RESTRICT |
| `fk_product_review_user` | `product_review.user_id` | `app_user.id` | CASCADE |
| `fk_product_review_product` | `product_review.product_id` | `product.id` | CASCADE |

> **CASCADE** — deleting the parent automatically deletes its children.  
> **RESTRICT** — cannot delete the parent while children exist.  
> **—** — no action on parent delete (orphan FK becomes NULL-able in practice).

---

## Entity Relationship Diagram (text)

```
app_user ──────────────────────────────────────────────────────────┐
  │ 1                                                               │
  ├──< user_address (user_id)                                       │
  ├──< payment_method (user_id) ──────────────────┐                │
  ├──< shopping_cart (user_id)                     │                │
  │       │ 1                                      │                │
  │       └──< cart_item (cart_id)                 │                │
  │               │ N                              │                │
  │       product >──────────────────────────── cart_item           │
  │         │ 1                                                      │
  │         ├──< order_item (product_id)                            │
  │         └──< product_review (product_id)                        │
  │                   │                                             │
  └───────────────────┘ (user_id)                                   │
                                                                    │
product_category ──< product (category_id)                         │
                                                                    │
shipping_method ──< customer_order (shipping_method_id)            │
payment_method  ──< customer_order (payment_method_id) ────────────┘
app_user        ──< customer_order (user_id)
                        │ 1
                        └──< order_item (order_id)
```

---

## Part 2 — Seed Data

Sample data for all 11 tables with correct FK references throughout.
Copy each block in order — dependencies are resolved top-to-bottom.

> **Passwords** — all hashed with BCrypt, plaintext: `password123`  
> **UUIDs** — fixed so every FK cross-reference is exact

> **Passwords** — all hashed with BCrypt, plaintext: `password123`  
> **UUIDs** — fixed so every FK cross-reference is exact

---

## Insertion Order (dependency chain)

```
app_user
  ├── user_address        (→ app_user)
  ├── payment_method      (→ app_user)
  └── shopping_cart       (→ app_user)
        └── cart_item     (→ shopping_cart, product)

product_category
  └── product             (→ product_category)
        ├── cart_item     (→ product)
        ├── order_item    (→ product)
        └── product_review(→ product, app_user)

shipping_method
customer_order            (→ app_user, payment_method, shipping_method)
  └── order_item          (→ customer_order, product)
```

---

## 1. `app_user`

> Depends on: nothing  
> Referenced by: `user_address`, `payment_method`, `shopping_cart`, `customer_order`, `product_review`

| Column | U1 — Admin | U2 — Customer 1 | U3 — Customer 2 |
|---|---|---|---|
| **id** | `a0000000-0000-0000-0000-000000000001` | `a0000000-0000-0000-0000-000000000002` | `a0000000-0000-0000-0000-000000000003` |
| email_address | admin@smartecommerce.com | john.doe@example.com | jane.smith@example.com |
| first_name | Admin | John | Jane |
| last_name | User | Doe | Smith |
| phone_number | +10000000001 | +10000000002 | +10000000003 |
| password_hash | `$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm` | same | same |
| is_active | true | true | true |
| role | `ADMIN` | `CUSTOMER` | `CUSTOMER` |
| oauth_provider | null | null | null |
| oauth_provider_id | null | null | null |

```sql
INSERT INTO app_user (id, email_address, first_name, last_name, phone_number, password_hash, is_active, role, created_at)
VALUES
  ('a0000000-0000-0000-0000-000000000001', 'admin@smartecommerce.com',  'Admin', 'User',  '+10000000001',
   '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'ADMIN',    NOW()),

  ('a0000000-0000-0000-0000-000000000002', 'john.doe@example.com',      'John',  'Doe',   '+10000000002',
   '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'CUSTOMER', NOW()),

  ('a0000000-0000-0000-0000-000000000003', 'jane.smith@example.com',    'Jane',  'Smith', '+10000000003',
   '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'CUSTOMER', NOW())

ON CONFLICT (email_address) DO NOTHING;
```

---

## 2. `product_category`

> Depends on: nothing  
> Referenced by: `product`

| Column | C1 | C2 | C3 |
|---|---|---|---|
| **id** | `c0000000-0000-0000-0000-000000000001` | `c0000000-0000-0000-0000-000000000002` | `c0000000-0000-0000-0000-000000000003` |
| category_name | Electronics | Clothing | Home & Garden |

```sql
INSERT INTO product_category (id, category_name, created_at)
VALUES
  ('c0000000-0000-0000-0000-000000000001', 'Electronics',   NOW()),
  ('c0000000-0000-0000-0000-000000000002', 'Clothing',      NOW()),
  ('c0000000-0000-0000-0000-000000000003', 'Home & Garden', NOW())

ON CONFLICT (category_name) DO NOTHING;
```

---

## 3. `product`

> Depends on: `product_category`  
> Referenced by: `cart_item`, `order_item`, `product_review`

| Column | P1 | P2 | P3 |
|---|---|---|---|
| **id** | `p0000000-0000-0000-0000-000000000001` | `p0000000-0000-0000-0000-000000000002` | `p0000000-0000-0000-0000-000000000003` |
| **category_id** → `product_category` | `c0000000-…-001` (Electronics) | `c0000000-…-002` (Clothing) | `c0000000-…-003` (Home & Garden) |
| name | Wireless Noise-Cancelling Headphones | Classic Slim-Fit T-Shirt | Stainless Steel Garden Trowel |
| description | Over-ear Bluetooth headphones, 30h battery, foldable design | 100% cotton, available in multiple colours | Rust-resistant, ergonomic grip handle |
| price | 149.99 | 24.99 | 12.49 |
| stock_quantity | 80 | 200 | 150 |
| is_active | true | true | true |
| images | `["headphones-front.jpg","headphones-side.jpg"]` | `["tshirt-white.jpg","tshirt-black.jpg"]` | `["trowel-main.jpg"]` |

```sql
INSERT INTO product (id, category_id, name, description, price, stock_quantity, is_active, images, created_at)
VALUES
  ('p0000000-0000-0000-0000-000000000001',
   'c0000000-0000-0000-0000-000000000001',
   'Wireless Noise-Cancelling Headphones',
   'Over-ear Bluetooth headphones, 30h battery, foldable design',
   149.99, 80, TRUE,
   '["headphones-front.jpg","headphones-side.jpg"]',
   NOW()),

  ('p0000000-0000-0000-0000-000000000002',
   'c0000000-0000-0000-0000-000000000002',
   'Classic Slim-Fit T-Shirt',
   '100% cotton, available in multiple colours',
   24.99, 200, TRUE,
   '["tshirt-white.jpg","tshirt-black.jpg"]',
   NOW()),

  ('p0000000-0000-0000-0000-000000000003',
   'c0000000-0000-0000-0000-000000000003',
   'Stainless Steel Garden Trowel',
   'Rust-resistant, ergonomic grip handle',
   12.49, 150, TRUE,
   '["trowel-main.jpg"]',
   NOW());
```

---

## 4. `user_address`

> Depends on: `app_user`  
> Referenced by: nothing

| Column | A1 | A2 | A3 |
|---|---|---|---|
| **id** | `ad000000-0000-0000-0000-000000000001` | `ad000000-0000-0000-0000-000000000002` | `ad000000-0000-0000-0000-000000000003` |
| **user_id** → `app_user` | `a0000000-…-001` (Admin) | `a0000000-…-002` (John) | `a0000000-…-003` (Jane) |
| address_line | 1 Commerce Plaza | 42 Maple Street | 7 Rosewood Avenue |
| city | San Francisco | Austin | New York |
| region | CA | TX | NY |
| country | USA | USA | USA |
| postal_code | 94105 | 78701 | 10001 |
| address_type | `BILLING` | `SHIPPING` | `SHIPPING` |
| is_default | true | true | true |

```sql
INSERT INTO user_address (id, user_id, address_line, city, region, country, postal_code, address_type, is_default, created_at)
VALUES
  ('ad000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001',
   '1 Commerce Plaza', 'San Francisco', 'CA', 'USA', '94105', 'BILLING',  TRUE, NOW()),

  ('ad000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000002',
   '42 Maple Street',  'Austin',        'TX', 'USA', '78701', 'SHIPPING', TRUE, NOW()),

  ('ad000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000003',
   '7 Rosewood Avenue','New York',      'NY', 'USA', '10001', 'SHIPPING', TRUE, NOW());
```

---

## 5. `payment_method`

> Depends on: `app_user`  
> Referenced by: `customer_order`

| Column | PM1 | PM2 | PM3 |
|---|---|---|---|
| **id** | `pm000000-0000-0000-0000-000000000001` | `pm000000-0000-0000-0000-000000000002` | `pm000000-0000-0000-0000-000000000003` |
| **user_id** → `app_user` | `a0000000-…-001` (Admin) | `a0000000-…-002` (John) | `a0000000-…-003` (Jane) |
| payment_type | `CREDIT_CARD` | `CREDIT_CARD` | `PAYPAL` |
| provider | Visa | Mastercard | PayPal |
| account_number | `****-****-****-4242` | `****-****-****-5555` | `jane.smith@paypal.com` |
| expiry_date | 2027-12-31 | 2026-08-31 | 2028-01-31 |
| is_default | true | true | true |
| is_active | true | true | true |

```sql
INSERT INTO payment_method (id, user_id, payment_type, provider, account_number, expiry_date, is_default, is_active, created_at)
VALUES
  ('pm000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001',
   'CREDIT_CARD', 'Visa',       '****-****-****-4242',    '2027-12-31 00:00:00+00', TRUE, TRUE, NOW()),

  ('pm000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000002',
   'CREDIT_CARD', 'Mastercard', '****-****-****-5555',    '2026-08-31 00:00:00+00', TRUE, TRUE, NOW()),

  ('pm000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000003',
   'PAYPAL',      'PayPal',     'jane.smith@paypal.com',  '2028-01-31 00:00:00+00', TRUE, TRUE, NOW());
```

---

## 6. `shipping_method`

> Depends on: nothing  
> Referenced by: `customer_order`

| Column | SM1 | SM2 | SM3 |
|---|---|---|---|
| **id** | `sm000000-0000-0000-0000-000000000001` | `sm000000-0000-0000-0000-000000000002` | `sm000000-0000-0000-0000-000000000003` |
| name | Standard Shipping | Express Shipping | Overnight Shipping |
| description | Delivered in 5–7 business days | Delivered in 2–3 business days | Next business day delivery |
| price | 4.99 | 12.99 | 24.99 |
| estimated_days | 7 | 3 | 1 |
| is_active | true | true | true |

```sql
INSERT INTO shipping_method (id, name, description, price, estimated_days, is_active, created_at)
VALUES
  ('sm000000-0000-0000-0000-000000000001',
   'Standard Shipping',  'Delivered in 5–7 business days',  4.99,  7, TRUE, NOW()),

  ('sm000000-0000-0000-0000-000000000002',
   'Express Shipping',   'Delivered in 2–3 business days', 12.99,  3, TRUE, NOW()),

  ('sm000000-0000-0000-0000-000000000003',
   'Overnight Shipping', 'Next business day delivery',     24.99,  1, TRUE, NOW());
```

---

## 7. `shopping_cart`

> Depends on: `app_user`  
> Referenced by: `cart_item`

| Column | SC1 | SC2 | SC3 |
|---|---|---|---|
| **id** | `sc000000-0000-0000-0000-000000000001` | `sc000000-0000-0000-0000-000000000002` | `sc000000-0000-0000-0000-000000000003` |
| **user_id** → `app_user` | `a0000000-…-001` (Admin) | `a0000000-…-002` (John) | `a0000000-…-003` (Jane) |

```sql
INSERT INTO shopping_cart (id, user_id, created_at)
VALUES
  ('sc000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', NOW()),
  ('sc000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', NOW()),
  ('sc000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', NOW());
```

---

## 8. `cart_item`

> Depends on: `shopping_cart`, `product`  
> Referenced by: nothing

| Column | CI1 | CI2 | CI3 |
|---|---|---|---|
| **id** | `ci000000-0000-0000-0000-000000000001` | `ci000000-0000-0000-0000-000000000002` | `ci000000-0000-0000-0000-000000000003` |
| **cart_id** → `shopping_cart` | `sc000000-…-002` (John's cart) | `sc000000-…-002` (John's cart) | `sc000000-…-003` (Jane's cart) |
| **product_id** → `product` | `p0000000-…-001` (Headphones) | `p0000000-…-002` (T-Shirt) | `p0000000-…-003` (Trowel) |
| quantity | 1 | 2 | 3 |

> John has two different products in his cart. Jane has one. Admin's cart is empty.  
> The `UNIQUE(cart_id, product_id)` constraint is satisfied — no cart has the same product twice.

```sql
INSERT INTO cart_item (id, cart_id, product_id, quantity, created_at)
VALUES
  ('ci000000-0000-0000-0000-000000000001',
   'sc000000-0000-0000-0000-000000000002',
   'p0000000-0000-0000-0000-000000000001',
   1, NOW()),

  ('ci000000-0000-0000-0000-000000000002',
   'sc000000-0000-0000-0000-000000000002',
   'p0000000-0000-0000-0000-000000000002',
   2, NOW()),

  ('ci000000-0000-0000-0000-000000000003',
   'sc000000-0000-0000-0000-000000000003',
   'p0000000-0000-0000-0000-000000000003',
   3, NOW());
```

---

## 9. `customer_order`

> Depends on: `app_user`, `payment_method`, `shipping_method`  
> Referenced by: `order_item`

| Column | O1 | O2 | O3 |
|---|---|---|---|
| **id** | `o0000000-0000-0000-0000-000000000001` | `o0000000-0000-0000-0000-000000000002` | `o0000000-0000-0000-0000-000000000003` |
| **user_id** → `app_user` | `a0000000-…-002` (John) | `a0000000-…-003` (Jane) | `a0000000-…-002` (John) |
| order_number | `ORD-2026-000001` | `ORD-2026-000002` | `ORD-2026-000003` |
| status | `delivered` | `processing` | `pending` |
| **payment_method_id** → `payment_method` | `pm000000-…-002` (John's Mastercard) | `pm000000-…-003` (Jane's PayPal) | `pm000000-…-002` (John's Mastercard) |
| **shipping_method_id** → `shipping_method` | `sm000000-…-001` (Standard) | `sm000000-…-002` (Express) | `sm000000-…-003` (Overnight) |
| subtotal | 149.99 | 24.99 | 37.47 |
| total | 154.98 | 37.98 | 62.46 |
| payment_status | `paid` | `paid` | `pending` |

> O3 total = 3 × trowel (37.47) + overnight shipping (24.99) = 62.46

```sql
INSERT INTO customer_order (id, user_id, order_number, status, payment_method_id, shipping_method_id, subtotal, total, payment_status, created_at)
VALUES
  ('o0000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000002',
   'ORD-2026-000001', 'delivered',
   'pm000000-0000-0000-0000-000000000002',
   'sm000000-0000-0000-0000-000000000001',
   149.99, 154.98, 'paid', NOW()),

  ('o0000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000003',
   'ORD-2026-000002', 'processing',
   'pm000000-0000-0000-0000-000000000003',
   'sm000000-0000-0000-0000-000000000002',
   24.99, 37.98, 'paid', NOW()),

  ('o0000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000002',
   'ORD-2026-000003', 'pending',
   'pm000000-0000-0000-0000-000000000002',
   'sm000000-0000-0000-0000-000000000003',
   37.47, 62.46, 'pending', NOW());
```

---

## 10. `order_item`

> Depends on: `customer_order`, `product`  
> Referenced by: nothing

| Column | OI1 | OI2 | OI3 |
|---|---|---|---|
| **id** | `oi000000-0000-0000-0000-000000000001` | `oi000000-0000-0000-0000-000000000002` | `oi000000-0000-0000-0000-000000000003` |
| **order_id** → `customer_order` | `o0000000-…-001` (John's order 1) | `o0000000-…-002` (Jane's order) | `o0000000-…-003` (John's order 2) |
| **product_id** → `product` | `p0000000-…-001` (Headphones) | `p0000000-…-002` (T-Shirt) | `p0000000-…-003` (Trowel) |
| unit_price | 149.99 | 24.99 | 12.49 |
| quantity | 1 | 1 | 3 |

```sql
INSERT INTO order_item (id, order_id, product_id, unit_price, quantity, created_at)
VALUES
  ('oi000000-0000-0000-0000-000000000001',
   'o0000000-0000-0000-0000-000000000001',
   'p0000000-0000-0000-0000-000000000001',
   149.99, 1, NOW()),

  ('oi000000-0000-0000-0000-000000000002',
   'o0000000-0000-0000-0000-000000000002',
   'p0000000-0000-0000-0000-000000000002',
   24.99, 1, NOW()),

  ('oi000000-0000-0000-0000-000000000003',
   'o0000000-0000-0000-0000-000000000003',
   'p0000000-0000-0000-0000-000000000003',
   12.49, 3, NOW());
```

---

## 11. `product_review`

> Depends on: `app_user`, `product`  
> Referenced by: nothing  
> Constraint: `UNIQUE(user_id, product_id)` — one review per user per product

| Column | R1 | R2 | R3 |
|---|---|---|---|
| **id** | `r0000000-0000-0000-0000-000000000001` | `r0000000-0000-0000-0000-000000000002` | `r0000000-0000-0000-0000-000000000003` |
| **user_id** → `app_user` | `a0000000-…-002` (John) | `a0000000-…-003` (Jane) | `a0000000-…-002` (John) |
| **product_id** → `product` | `p0000000-…-001` (Headphones) | `p0000000-…-002` (T-Shirt) | `p0000000-…-003` (Trowel) |
| rating | 5 | 4 | 3 |
| comment | Incredible sound quality, worth every penny! | Great fit and fabric, runs slightly small. | Does the job but the handle could be more comfortable. |

> Each `(user_id, product_id)` pair is unique — John reviews Headphones and Trowel; Jane reviews T-Shirt. No duplicates.

```sql
INSERT INTO product_review (id, user_id, product_id, rating, comment, created_at)
VALUES
  ('r0000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000002',
   'p0000000-0000-0000-0000-000000000001',
   5, 'Incredible sound quality, worth every penny!', NOW()),

  ('r0000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000003',
   'p0000000-0000-0000-0000-000000000002',
   4, 'Great fit and fabric, runs slightly small.', NOW()),

  ('r0000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000002',
   'p0000000-0000-0000-0000-000000000003',
   3, 'Does the job but the handle could be more comfortable.', NOW());
```

---

## Full Combined Script (run in this order)

```sql
-- ============================================================
-- Smart E-Commerce Security — Complete Seed Data
-- Run once against a clean or empty database
-- All passwords = BCrypt of "password123"
-- ============================================================

-- 1. Users
INSERT INTO app_user (id, email_address, first_name, last_name, phone_number, password_hash, is_active, role, created_at)
VALUES
  ('a0000000-0000-0000-0000-000000000001', 'admin@smartecommerce.com', 'Admin', 'User',  '+10000000001', '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'ADMIN',    NOW()),
  ('a0000000-0000-0000-0000-000000000002', 'john.doe@example.com',     'John',  'Doe',   '+10000000002', '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'CUSTOMER', NOW()),
  ('a0000000-0000-0000-0000-000000000003', 'jane.smith@example.com',   'Jane',  'Smith', '+10000000003', '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'CUSTOMER', NOW())
ON CONFLICT (email_address) DO NOTHING;

-- 2. Categories
INSERT INTO product_category (id, category_name, created_at)
VALUES
  ('c0000000-0000-0000-0000-000000000001', 'Electronics',   NOW()),
  ('c0000000-0000-0000-0000-000000000002', 'Clothing',      NOW()),
  ('c0000000-0000-0000-0000-000000000003', 'Home & Garden', NOW())
ON CONFLICT (category_name) DO NOTHING;

-- 3. Products
INSERT INTO product (id, category_id, name, description, price, stock_quantity, is_active, images, created_at)
VALUES
  ('p0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Wireless Noise-Cancelling Headphones', 'Over-ear Bluetooth headphones, 30h battery, foldable design', 149.99, 80,  TRUE, '["headphones-front.jpg","headphones-side.jpg"]', NOW()),
  ('p0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'Classic Slim-Fit T-Shirt',             '100% cotton, available in multiple colours',                   24.99, 200, TRUE, '["tshirt-white.jpg","tshirt-black.jpg"]',        NOW()),
  ('p0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'Stainless Steel Garden Trowel',        'Rust-resistant, ergonomic grip handle',                        12.49, 150, TRUE, '["trowel-main.jpg"]',                           NOW());

-- 4. Addresses
INSERT INTO user_address (id, user_id, address_line, city, region, country, postal_code, address_type, is_default, created_at)
VALUES
  ('ad000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '1 Commerce Plaza', 'San Francisco', 'CA', 'USA', '94105', 'BILLING',  TRUE, NOW()),
  ('ad000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', '42 Maple Street',  'Austin',        'TX', 'USA', '78701', 'SHIPPING', TRUE, NOW()),
  ('ad000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', '7 Rosewood Avenue','New York',      'NY', 'USA', '10001', 'SHIPPING', TRUE, NOW());

-- 5. Payment Methods
INSERT INTO payment_method (id, user_id, payment_type, provider, account_number, expiry_date, is_default, is_active, created_at)
VALUES
  ('pm000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'CREDIT_CARD', 'Visa',       '****-****-****-4242',   '2027-12-31 00:00:00+00', TRUE, TRUE, NOW()),
  ('pm000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'CREDIT_CARD', 'Mastercard', '****-****-****-5555',   '2026-08-31 00:00:00+00', TRUE, TRUE, NOW()),
  ('pm000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'PAYPAL',      'PayPal',     'jane.smith@paypal.com', '2028-01-31 00:00:00+00', TRUE, TRUE, NOW());

-- 6. Shipping Methods
INSERT INTO shipping_method (id, name, description, price, estimated_days, is_active, created_at)
VALUES
  ('sm000000-0000-0000-0000-000000000001', 'Standard Shipping',  'Delivered in 5–7 business days',  4.99,  7, TRUE, NOW()),
  ('sm000000-0000-0000-0000-000000000002', 'Express Shipping',   'Delivered in 2–3 business days', 12.99,  3, TRUE, NOW()),
  ('sm000000-0000-0000-0000-000000000003', 'Overnight Shipping', 'Next business day delivery',     24.99,  1, TRUE, NOW());

-- 7. Shopping Carts
INSERT INTO shopping_cart (id, user_id, created_at)
VALUES
  ('sc000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', NOW()),
  ('sc000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', NOW()),
  ('sc000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', NOW());

-- 8. Cart Items
INSERT INTO cart_item (id, cart_id, product_id, quantity, created_at)
VALUES
  ('ci000000-0000-0000-0000-000000000001', 'sc000000-0000-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000001', 1, NOW()),
  ('ci000000-0000-0000-0000-000000000002', 'sc000000-0000-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000002', 2, NOW()),
  ('ci000000-0000-0000-0000-000000000003', 'sc000000-0000-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000003', 3, NOW());

-- 9. Orders
INSERT INTO customer_order (id, user_id, order_number, status, payment_method_id, shipping_method_id, subtotal, total, payment_status, created_at)
VALUES
  ('o0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'ORD-2026-000001', 'delivered',  'pm000000-0000-0000-0000-000000000002', 'sm000000-0000-0000-0000-000000000001', 149.99, 154.98, 'paid',    NOW()),
  ('o0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', 'ORD-2026-000002', 'processing', 'pm000000-0000-0000-0000-000000000003', 'sm000000-0000-0000-0000-000000000002',  24.99,  37.98, 'paid',    NOW()),
  ('o0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', 'ORD-2026-000003', 'pending',    'pm000000-0000-0000-0000-000000000002', 'sm000000-0000-0000-0000-000000000003',  37.47,  62.46, 'pending', NOW());

-- 10. Order Items
INSERT INTO order_item (id, order_id, product_id, unit_price, quantity, created_at)
VALUES
  ('oi000000-0000-0000-0000-000000000001', 'o0000000-0000-0000-0000-000000000001', 'p0000000-0000-0000-0000-000000000001', 149.99, 1, NOW()),
  ('oi000000-0000-0000-0000-000000000002', 'o0000000-0000-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000002',  24.99, 1, NOW()),
  ('oi000000-0000-0000-0000-000000000003', 'o0000000-0000-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000003',  12.49, 3, NOW());

-- 11. Reviews
INSERT INTO product_review (id, user_id, product_id, rating, comment, created_at)
VALUES
  ('r0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000001', 5, 'Incredible sound quality, worth every penny!',            NOW()),
  ('r0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', 'p0000000-0000-0000-0000-000000000002', 4, 'Great fit and fabric, runs slightly small.',               NOW()),
  ('r0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', 'p0000000-0000-0000-0000-000000000003', 3, 'Does the job but the handle could be more comfortable.',   NOW());
```

---

## FK Reference Map

```
app_user (a0000000-…-001/002/003)
  ├─ user_address.user_id          ad000000-…-001 → 001, 002 → 002, 003 → 003
  ├─ payment_method.user_id        pm000000-…-001 → 001, 002 → 002, 003 → 003
  ├─ shopping_cart.user_id         sc000000-…-001 → 001, 002 → 002, 003 → 003
  ├─ customer_order.user_id        o0000000-…-001 → 002, 002 → 003, 003 → 002
  └─ product_review.user_id        r0000000-…-001 → 002, 002 → 003, 003 → 002

product_category (c0000000-…-001/002/003)
  └─ product.category_id           p0000000-…-001 → 001, 002 → 002, 003 → 003

product (p0000000-…-001/002/003)
  ├─ cart_item.product_id          ci000000-…-001 → 001, 002 → 002, 003 → 003
  ├─ order_item.product_id         oi000000-…-001 → 001, 002 → 002, 003 → 003
  └─ product_review.product_id     r0000000-…-001 → 001, 002 → 002, 003 → 003

shopping_cart (sc000000-…-001/002/003)
  └─ cart_item.cart_id             ci000000-…-001 → 002, 002 → 002, 003 → 003

payment_method (pm000000-…-001/002/003)
  └─ customer_order.payment_method_id   o0000000-…-001 → 002, 002 → 003, 003 → 002

shipping_method (sm000000-…-001/002/003)
  └─ customer_order.shipping_method_id  o0000000-…-001 → 001, 002 → 002, 003 → 003

customer_order (o0000000-…-001/002/003)
  └─ order_item.order_id           oi000000-…-001 → 001, 002 → 002, 003 → 003
```

