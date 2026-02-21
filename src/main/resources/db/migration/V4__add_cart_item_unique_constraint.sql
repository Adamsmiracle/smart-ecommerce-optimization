-- Add unique constraint to cart_item table for proper UPSERT operations
-- This constraint is required for the ON CONFLICT (cart_id, product_id) clause
-- used in the CartRepositoryImpl.addItem() method

ALTER TABLE cart_item 
ADD CONSTRAINT uq_cart_item_cart_product 
UNIQUE (cart_id, product_id);
