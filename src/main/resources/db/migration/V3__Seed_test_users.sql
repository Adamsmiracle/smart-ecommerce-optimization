-- ============================================================
-- V3: Seed test users for ADMIN and CUSTOMER roles
-- All passwords are BCrypt hashes of "password123"
-- ============================================================

INSERT INTO app_user (email_address, first_name, last_name, phone_number, password_hash, is_active, role, created_at)
VALUES
  ('admin@smartecommerce.com', 'Admin', 'User', '+10000000001',
   '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'ADMIN', NOW()),

  ('john.doe@example.com', 'John', 'Doe', '+10000000002',
   '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'CUSTOMER', NOW()),

  ('jane.smith@example.com', 'Jane', 'Smith', '+10000000003',
   '$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm', TRUE, 'CUSTOMER', NOW())

ON CONFLICT (email_address) DO NOTHING;
