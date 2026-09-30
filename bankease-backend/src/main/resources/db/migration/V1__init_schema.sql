-- ============================================
-- V1__init_schema.sql
-- BankEase: NextGen Net Banking - Initial Schema
-- ============================================

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    phone_number    VARCHAR(20)   NOT NULL UNIQUE,
    role            VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    account_number  VARCHAR(20)   NOT NULL UNIQUE,
    account_type    VARCHAR(20)   NOT NULL DEFAULT 'SAVINGS',
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'INR',
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id          BIGINT        NOT NULL,
    related_account_id  BIGINT        NULL,
    transaction_type    VARCHAR(20)   NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    balance_after        DECIMAL(15,2) NOT NULL,
    description         VARCHAR(255)  NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_related_account FOREIGN KEY (related_account_id) REFERENCES accounts(id)
) ENGINE=InnoDB;

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);