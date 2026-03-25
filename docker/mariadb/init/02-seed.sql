-- ============================================================
-- Balance Portal — Seed Data
-- ============================================================
-- Run AFTER schema.sql:
--   mysql -u portaluser -p balance_portal < seed.sql
--
-- Passwords (BCrypt cost 12):
--   jsmith   → pass123
--   mjohnson → secure99
--   bdavis   → letmein
-- ============================================================

USE balance_portal;

-- ── Customers ────────────────────────────────────────────────

INSERT INTO customers (account_number, username, password_hash, first_name, last_name, email, verified)
VALUES
('ACC-1001', 'jsmith',
 '$2a$12$KIXbPHDjHqBv5e3RQ5HvZuPl5VEMCLpFWfyI7h7HmtPLjQNqeJjSe',
 'John', 'Smith', 'jsmith@email.com', 1),

('ACC-1002', 'mjohnson',
 '$2a$12$7ZqNgr.T9.3f1xWEJPkxoeTZm5BnAOoUvlBM3SOVcE.NeG4cIxsKS',
 'Maria', 'Johnson', 'mjohnson@email.com', 1),

('ACC-1003', 'bdavis',
 '$2a$12$2aFz2u9LGvkT8xlO1l3xo.kMO.F9ABiGFxJp7.yVxhGMCBE7KLqjm',
 'Brian', 'Davis', 'bdavis@email.com', 1);


-- ── Invoices for ACC-1001 (John Smith) ───────────────────────

INSERT INTO invoices (invoice_number, account_number, description, invoice_date, due_date, amount, status)
VALUES
('INV-5001', 'ACC-1001', 'Monthly Service Fee',   '2025-01-15', '2025-02-15',   150.00, 'PAID'),
('INV-5002', 'ACC-1001', 'Equipment Rental',       '2025-02-10', '2025-03-10',   320.50, 'PAID'),
('INV-5003', 'ACC-1001', 'Support Package - Q1',   '2025-03-01', '2025-04-01',   499.99, 'OVERDUE'),
('INV-5004', 'ACC-1001', 'Consulting Hours',        '2025-04-05', '2025-05-05',   875.00, 'UNPAID'),
('INV-5005', 'ACC-1001', 'License Renewal',         '2025-05-20', '2025-06-20',  1200.00, 'PENDING');


-- ── Invoices for ACC-1002 (Maria Johnson) ────────────────────

INSERT INTO invoices (invoice_number, account_number, description, invoice_date, due_date, amount, status)
VALUES
('INV-6001', 'ACC-1002', 'Annual Subscription',     '2025-01-01', '2025-01-31',  2400.00, 'PAID'),
('INV-6002', 'ACC-1002', 'Implementation Services', '2025-02-14', '2025-03-14',  3150.75, 'OVERDUE'),
('INV-6003', 'ACC-1002', 'Training Session',         '2025-03-22', '2025-04-22',   600.00, 'UNPAID'),
('INV-6004', 'ACC-1002', 'Hardware Maintenance',     '2025-04-30', '2025-05-30',   445.00, 'PENDING');


-- ── Invoices for ACC-1003 (Brian Davis) ──────────────────────

INSERT INTO invoices (invoice_number, account_number, description, invoice_date, due_date, amount, status)
VALUES
('INV-7001', 'ACC-1003', 'Starter Plan - Jan',   '2025-01-05', '2025-02-05',  89.99, 'PAID'),
('INV-7002', 'ACC-1003', 'Starter Plan - Feb',   '2025-02-05', '2025-03-05',  89.99, 'PAID'),
('INV-7003', 'ACC-1003', 'Starter Plan - Mar',   '2025-03-05', '2025-04-05',  89.99, 'PAID'),
('INV-7004', 'ACC-1003', 'Upgrade to Pro Plan',  '2025-04-10', '2025-05-10', 250.00, 'UNPAID'),
('INV-7005', 'ACC-1003', 'Pro Plan - Apr',        '2025-04-10', '2025-05-10', 199.99, 'OVERDUE'),
('INV-7006', 'ACC-1003', 'Pro Plan - May',        '2025-05-10', '2025-06-10', 199.99, 'PENDING');
