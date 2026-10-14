--liquibase formatted sql

--changeset robert_neat:payment_card-001
CREATE TABLE payment_card (
    id VARCHAR(10) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    masked_number VARCHAR(19) NOT NULL,
    network VARCHAR(12) NOT NULL CHECK (network IN ('Visa', 'Mastercard')),
    product VARCHAR(50) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('active', 'blocked', 'pending', 'expired')),
    expiry VARCHAR(7) NOT NULL,
    limit_daily NUMERIC(19, 2) NOT NULL,
    limit_atm NUMERIC(19, 2) NOT NULL
);

--changeset robert_neat:card_authorization_ref-001
CREATE TABLE card_authorization_ref (
    id VARCHAR(36) PRIMARY KEY,
    card_id VARCHAR(10) NOT NULL REFERENCES payment_card (id),
    merchant VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processor VARCHAR(12) NOT NULL CHECK (processor IN ('Visa', 'Mastercard')),
    approved BOOLEAN NOT NULL
);

--changeset robert_neat:chargeback_ref-001
CREATE TABLE chargeback_ref (
    id VARCHAR(36) PRIMARY KEY,
    card_authorization_ref_id VARCHAR(36) NOT NULL REFERENCES card_authorization_ref (id),
    reason_code VARCHAR(4) NOT NULL CHECK (reason_code IN ('4855', '4837', '4853', '4863')),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('FILED', 'UNDER_REVIEW', 'RESOLVED_CUSTOMER', 'RESOLVED_MERCHANT')),
    filed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
