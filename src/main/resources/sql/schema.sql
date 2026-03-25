-- ============================================================
-- Balance Portal — MariaDB Schema
-- ============================================================
-- Run this once to set up the database:
--   mysql -u root -p < schema.sql
-- Or paste into MySQL Workbench / DBeaver / HeidiSQL
-- ============================================================

CREATE DATABASE IF NOT EXISTS balance_portal
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE balance_portal;

-- ── customers ────────────────────────────────────────────────
-- One row per registered user.
-- password_hash stores the full BCrypt string (e.g. $2a$12$...)
-- verified = 0 means email not yet confirmed
-- created_at is set automatically on INSERT

CREATE TABLE IF NOT EXISTS customers (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20)     NOT NULL,
    username       VARCHAR(20)     NOT NULL,
    password_hash  VARCHAR(72)     NOT NULL,   -- BCrypt output is always 60 chars; 72 is safe
    first_name     VARCHAR(50)     NOT NULL,
    last_name      VARCHAR(50)     NOT NULL,
    email          VARCHAR(255)    NOT NULL,
    verified       TINYINT(1)      NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_account_number (account_number),
    UNIQUE KEY uq_username       (username),
    UNIQUE KEY uq_email          (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ── invoices ─────────────────────────────────────────────────
-- One row per invoice, linked to customers via account_number.
-- status uses an ENUM so the DB enforces valid values.

CREATE TABLE IF NOT EXISTS invoices (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(20)     NOT NULL,
    account_number VARCHAR(20)     NOT NULL,
    description    VARCHAR(255)    NOT NULL,
    invoice_date   DATE            NOT NULL,
    due_date       DATE            NOT NULL,
    amount         DECIMAL(12, 2)  NOT NULL,
    status         ENUM('PAID','UNPAID','OVERDUE','PENDING') NOT NULL DEFAULT 'UNPAID',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_invoice_number (invoice_number),
    INDEX idx_account_number     (account_number),

    CONSTRAINT fk_invoice_customer
        FOREIGN KEY (account_number)
        REFERENCES customers (account_number)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ── pending_registrations ─────────────────────────────────────
-- Stores unverified registrations until the email link is clicked.
-- expires_at allows the app to reject stale tokens.
-- This table is cleaned up by VerifyServlet on use,
-- and can be periodically purged by a scheduled job in production.

CREATE TABLE IF NOT EXISTS pending_registrations (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    token          VARCHAR(36)     NOT NULL,   -- UUID
    username       VARCHAR(20)     NOT NULL,
    password_hash  VARCHAR(72)     NOT NULL,
    first_name     VARCHAR(50)     NOT NULL,
    last_name      VARCHAR(50)     NOT NULL,
    email          VARCHAR(255)    NOT NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at     DATETIME        NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_token    (token),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- Create application user
-- Use this account in your env vars — never use root
-- ============================================================

CREATE USER IF NOT EXISTS 'portaluser'@'localhost'
    IDENTIFIED BY 'PortalPass123!';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON balance_portal.*
    TO 'portaluser'@'localhost';

FLUSH PRIVILEGES;
