-- ============================================================
-- Balance Portal — Docker MariaDB Init Schema
-- The database and user are created by Docker env vars.
-- This script only creates the tables.
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20)     NOT NULL,
    username       VARCHAR(20)     NOT NULL,
    password_hash  VARCHAR(72)     NOT NULL,
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


CREATE TABLE IF NOT EXISTS pending_registrations (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    token          VARCHAR(36)     NOT NULL,
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
