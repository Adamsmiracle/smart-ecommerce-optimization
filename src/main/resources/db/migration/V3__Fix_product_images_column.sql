-- Fix images column type for Product table
-- Change from JSONB to TEXT to work with JPA converter

ALTER TABLE product ALTER COLUMN images TYPE TEXT USING images::TEXT;
