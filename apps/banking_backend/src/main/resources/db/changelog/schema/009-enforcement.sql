--liquibase formatted sql

--changeset robert_neat:enforcement_case-001
CREATE TABLE enforcement_case (
    id VARCHAR(10) PRIMARY KEY,
    gateway_enforcement_id VARCHAR(36) UNIQUE,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    bank_account_id VARCHAR(20) REFERENCES bank_account (id),
    bailiff VARCHAR(200) NOT NULL,
    case_number VARCHAR(50) NOT NULL,
    kind VARCHAR(15) NOT NULL CHECK (kind IN ('ALIMONY', 'NON_ALIMONY')),
    blocked_amount NUMERIC(19, 2) NOT NULL,
    transferred_amount NUMERIC(19, 2),
    protected_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('registered', 'active', 'settled', 'closed')),
    approval_id VARCHAR(36)
);
