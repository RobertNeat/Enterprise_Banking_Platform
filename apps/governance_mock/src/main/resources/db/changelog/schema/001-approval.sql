--liquibase formatted sql

--changeset robert_neat:approval_request-001
CREATE TABLE governance.approval_request (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    requested_by VARCHAR(50) NOT NULL,
    payload VARCHAR,
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_by VARCHAR(50),
    decided_at TIMESTAMP WITH TIME ZONE
);
