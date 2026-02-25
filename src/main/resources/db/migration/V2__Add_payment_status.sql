-- Add payment_status column to customer_order
ALTER TABLE customer_order
    ADD COLUMN payment_status VARCHAR(30) DEFAULT 'pending';

-- Add index for payment_status for faster lookups
CREATE INDEX IF NOT EXISTS idx_customer_order_payment_status ON customer_order(payment_status);

