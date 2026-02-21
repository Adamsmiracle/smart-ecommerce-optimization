-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =======================
-- Users
-- =======================
CREATE TABLE app_user (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          email_address VARCHAR(255) NOT NULL UNIQUE,
                          first_name VARCHAR(100),
                          last_name VARCHAR(100),
                          phone_number VARCHAR(20),
                          password_hash VARCHAR(255) NOT NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          roles VARCHAR(50)
);

-- =======================
-- Addresses
-- =======================
CREATE TABLE address (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                         address_line VARCHAR(255),
                         city VARCHAR(100),
                         region VARCHAR(100),
                         country VARCHAR(100),
                         postal_code VARCHAR(20),
                         address_type VARCHAR(50),
                         created_at TIMESTAMP DEFAULT NOW()
);

-- =======================
-- Product Categories
-- =======================
CREATE TABLE product_category (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  category_name VARCHAR(100) NOT NULL UNIQUE,
                                  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =======================
-- Products
-- =======================
CREATE TABLE product (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         category_id UUID NOT NULL
                             REFERENCES product_category(id) ON DELETE RESTRICT,
                         name VARCHAR(255) NOT NULL,
                         description TEXT,
                         price NUMERIC(10, 2) NOT NULL,
                         stock_quantity INT NOT NULL DEFAULT 0,
                         is_active BOOLEAN DEFAULT TRUE,
                         images JSONB DEFAULT '[]',
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =======================
-- Product Reviews
-- =======================
CREATE TABLE product_review (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL
                                    REFERENCES app_user(id) ON DELETE CASCADE,
                                product_id UUID NOT NULL
                                    REFERENCES product(id) ON DELETE CASCADE,
                                rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                                comment TEXT,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =======================
-- Shopping Cart
-- =======================
CREATE TABLE shopping_cart (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL
                                   REFERENCES app_user(id) ON DELETE CASCADE,
                               created_at TIMESTAMP DEFAULT NOW()
);

-- =======================
-- Cart Items
-- =======================
CREATE TABLE cart_item (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           cart_id UUID NOT NULL
                               REFERENCES shopping_cart(id) ON DELETE CASCADE,
                           product_id UUID NOT NULL
                               REFERENCES product(id) ON DELETE CASCADE,
                           quantity INT NOT NULL DEFAULT 1
);

-- =======================
-- Payment Methods
-- =======================
CREATE TABLE payment_method (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL
                                    REFERENCES app_user(id) ON DELETE CASCADE,
                                payment_type VARCHAR(50),
                                provider VARCHAR(100),
                                account_number VARCHAR(100),
                                expiry_date TIMESTAMP,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =======================
-- Shipping Methods
-- =======================
CREATE TABLE shipping_method (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 name VARCHAR(100) NOT NULL,
                                 description TEXT,
                                 price NUMERIC(10, 2),
                                 estimated_days INT,
                                 created_at TIMESTAMP DEFAULT NOW()
);

-- =======================
-- Customer Orders
-- =======================
CREATE TABLE customer_order (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL
                                    REFERENCES app_user(id) ON DELETE CASCADE,
                                order_number VARCHAR(50) UNIQUE NOT NULL,
                                status VARCHAR(50) DEFAULT 'pending',

                                payment_method_id UUID
                                    REFERENCES payment_method(id) ON DELETE SET NULL,

                                shipping_method_id UUID
                                    REFERENCES shipping_method(id) ON DELETE SET NULL,

                                payment_status VARCHAR(30),
                                subtotal NUMERIC(10, 2),
                                total_amount NUMERIC(10, 2),
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =======================
-- Order Items
-- =======================
CREATE TABLE order_item (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            order_id UUID NOT NULL
                                REFERENCES customer_order(id) ON DELETE CASCADE,
                            product_id UUID NOT NULL
                                REFERENCES product(id) ON DELETE RESTRICT,
                            unit_price NUMERIC(10, 2) NOT NULL,
                            quantity INT NOT NULL DEFAULT 1
);


-- ============================
-- ADDRESS
-- ============================
CREATE INDEX idx_address_user_id
    ON address(user_id);

-- ============================
-- PRODUCT
-- ============================
CREATE INDEX idx_product_category_id
    ON product(category_id);

-- ============================
-- PRODUCT REVIEW
-- ============================
CREATE INDEX idx_product_review_user_id
    ON product_review(user_id);

CREATE INDEX idx_product_review_product_id
    ON product_review(product_id);

-- ============================
-- SHOPPING CART
-- ============================
CREATE INDEX idx_shopping_cart_user_id
    ON shopping_cart(user_id);

-- ============================
-- CART ITEM
-- ============================
CREATE INDEX idx_cart_item_cart_id
    ON cart_item(cart_id);

CREATE INDEX idx_cart_item_product_id
    ON cart_item(product_id);

-- ============================
-- PAYMENT METHOD
-- ============================
CREATE INDEX idx_payment_method_user_id
    ON payment_method(user_id);

-- ============================
-- CUSTOMER ORDER
-- ============================
CREATE INDEX idx_customer_order_user_id
    ON customer_order(user_id);

CREATE INDEX idx_customer_order_payment_method_id
    ON customer_order(payment_method_id);

CREATE INDEX idx_customer_order_shipping_method_id
    ON customer_order(shipping_method_id);

-- ============================
-- ORDER ITEM
-- ============================
CREATE INDEX idx_order_item_order_id
    ON order_item(order_id);

CREATE INDEX idx_order_item_product_id
    ON order_item(product_id);
