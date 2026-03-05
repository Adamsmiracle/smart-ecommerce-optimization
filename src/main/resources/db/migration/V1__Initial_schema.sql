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
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- User Addresses
-- =======================
CREATE TABLE user_address (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    address_type VARCHAR(50),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Product Categories
-- =======================
CREATE TABLE product_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Products
-- (images stored as TEXT to work with JPA converter)
-- =======================
CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(19,2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    images TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Shopping Carts
-- =======================
CREATE TABLE shopping_cart (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Cart Items
-- =======================
CREATE TABLE cart_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(cart_id, product_id)
);

-- =======================
-- Payment Methods
-- =======================
CREATE TABLE payment_method (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    account_number VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Shipping Methods
-- =======================
CREATE TABLE shipping_method (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(19,2) NOT NULL CHECK (price >= 0),
    estimated_days INTEGER CHECK (estimated_days >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Customer Orders
-- (payment_status included from the start)
-- =======================
CREATE TABLE customer_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    payment_method_id UUID,
    shipping_method_id UUID,
    subtotal DECIMAL(19,2) NOT NULL CHECK (subtotal >= 0),
    total DECIMAL(19,2) NOT NULL CHECK (total >= 0),
    payment_status VARCHAR(30) DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Order Items
-- =======================
CREATE TABLE order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL CHECK (unit_price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Product Reviews
-- =======================
CREATE TABLE product_review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- =======================
-- Foreign Key Constraints
-- =======================
ALTER TABLE user_address ADD CONSTRAINT fk_user_address_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

ALTER TABLE product ADD CONSTRAINT fk_product_category
    FOREIGN KEY (category_id) REFERENCES product_category(id) ON DELETE RESTRICT;

ALTER TABLE shopping_cart ADD CONSTRAINT fk_shopping_cart_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

ALTER TABLE cart_item ADD CONSTRAINT fk_cart_item_cart
    FOREIGN KEY (cart_id) REFERENCES shopping_cart(id) ON DELETE CASCADE;

ALTER TABLE cart_item ADD CONSTRAINT fk_cart_item_product
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

ALTER TABLE customer_order ADD CONSTRAINT fk_customer_order_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT;

ALTER TABLE customer_order ADD CONSTRAINT fk_customer_order_payment_method
    FOREIGN KEY (payment_method_id) REFERENCES payment_method(id);

ALTER TABLE customer_order ADD CONSTRAINT fk_customer_order_shipping_method
    FOREIGN KEY (shipping_method_id) REFERENCES shipping_method(id);

ALTER TABLE order_item ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id) REFERENCES customer_order(id) ON DELETE CASCADE;

ALTER TABLE order_item ADD CONSTRAINT fk_order_item_product
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE RESTRICT;

ALTER TABLE payment_method ADD CONSTRAINT fk_payment_method_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

ALTER TABLE product_review ADD CONSTRAINT fk_product_review_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

ALTER TABLE product_review ADD CONSTRAINT fk_product_review_product
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

-- =======================
-- Indexes
-- =======================
CREATE INDEX idx_user_email ON app_user(email_address);
CREATE INDEX idx_user_role ON app_user(role);

CREATE INDEX idx_user_address_user_id ON user_address(user_id);
CREATE INDEX idx_address_user ON user_address(user_id);

CREATE INDEX idx_product_category_id ON product(category_id);
CREATE INDEX idx_product_is_active ON product(is_active);

CREATE INDEX idx_shopping_cart_user_id ON shopping_cart(user_id);

CREATE INDEX idx_cart_item_cart_id ON cart_item(cart_id);
CREATE INDEX idx_cart_item_product_id ON cart_item(product_id);

CREATE INDEX idx_customer_order_user_id ON customer_order(user_id);
CREATE INDEX idx_customer_order_status ON customer_order(status);
CREATE INDEX idx_customer_order_order_number ON customer_order(order_number);
CREATE INDEX idx_customer_order_payment_status ON customer_order(payment_status);

CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_product_id ON order_item(product_id);

CREATE INDEX idx_payment_method_user_id ON payment_method(user_id);
CREATE INDEX idx_payment_method_is_active ON payment_method(is_active);

CREATE INDEX idx_shipping_method_is_active ON shipping_method(is_active);

CREATE INDEX idx_product_review_user_id ON product_review(user_id);
CREATE INDEX idx_product_review_product_id ON product_review(product_id);
CREATE INDEX idx_product_review_rating ON product_review(rating);

-- =======================
-- Composite Indexes for Query Optimization
-- Improve performance of frequently filtered multi-column queries
-- =======================

-- Products: category + active status filter (used in getProductsByCategory, getActiveProducts)
CREATE INDEX idx_product_category_active ON product(category_id, is_active);

-- Products: price range filter (used in getProductsByPriceRange)
CREATE INDEX idx_product_price_range ON product(price);

-- Products: case-insensitive name search on active products (used in search, multi-criteria)
CREATE INDEX idx_product_name_active ON product(LOWER(name), is_active) WHERE is_active = TRUE;

-- Orders: user + status composite (used in getOrdersByUserId, getOrdersByStatus, getOrdersByUserAndStatus)
CREATE INDEX idx_customer_order_user_status ON customer_order(user_id, status);

-- Orders: created_at descending (used in order history / reporting)
CREATE INDEX idx_customer_order_created_at ON customer_order(created_at DESC);

-- Users: email + active status composite (used in auth filter user lookup)
CREATE INDEX idx_user_email_active ON app_user(email_address, is_active);

-- Reviews: unique user-product pair (used in duplicate review check)
CREATE UNIQUE INDEX idx_product_review_user_product ON product_review(user_id, product_id);

