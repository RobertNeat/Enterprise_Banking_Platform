--liquibase formatted sql

--changeset robert_neat:enforcement_case-001
-- id doubles as the gateway-side "gatewayEnforcementId" referenced by banking_backend's
-- enforcement_case seed (db/changelog/seed/009-enforcement.sql) -- no separate id column.
CREATE TABLE gateway.enforcement_case (
    id VARCHAR(36) PRIMARY KEY,
    customer VARCHAR(50) NOT NULL,
    bailiff VARCHAR(150) NOT NULL,
    case_number VARCHAR(50) NOT NULL,
    blocked_amount NUMERIC(19, 2) NOT NULL,
    transferred_amount NUMERIC(19, 2),
    protected_amount NUMERIC(19, 2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('REGISTERED', 'ACTIVE', 'SETTLED', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    settled_at TIMESTAMP WITH TIME ZONE
);
