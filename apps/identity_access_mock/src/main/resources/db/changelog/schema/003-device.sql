--liquibase formatted sql

--changeset robert_neat:device-001
-- owner_id is FK-like but intentionally not a hard REFERENCES: it may point at an employeeId
-- (identity_access.employee_account) or an external e-banking customer/user id (EB-xxxx) that
-- doesn't exist in this schema at all.
CREATE TABLE identity_access.device (
    device_id VARCHAR(50) PRIMARY KEY,
    owner_id VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    trusted BOOLEAN NOT NULL
);

--changeset robert_neat:device-002
CREATE INDEX ix_device_owner_id ON identity_access.device (owner_id);
