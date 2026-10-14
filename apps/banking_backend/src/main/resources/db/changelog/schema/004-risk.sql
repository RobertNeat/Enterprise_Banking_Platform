--liquibase formatted sql

--changeset robert_neat:collateral_entry-001
CREATE TABLE collateral_entry (
    id VARCHAR(10) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('mortgage', 'pledge', 'guarantee')),
    valuation NUMERIC(19, 2) NOT NULL,
    ltv NUMERIC(5, 2) NOT NULL,
    rating VARCHAR(10) NOT NULL,
    stage VARCHAR(10) NOT NULL CHECK (stage IN ('stage1', 'stage2', 'stage3')),
    provision NUMERIC(19, 2) NOT NULL,
    ecl_classified_at TIMESTAMP WITH TIME ZONE
);
