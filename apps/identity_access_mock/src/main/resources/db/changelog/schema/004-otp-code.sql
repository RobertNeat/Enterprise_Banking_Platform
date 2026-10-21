--liquibase formatted sql

--changeset robert_neat:otp_code-001
CREATE TABLE identity_access.otp_code (
    otp_id VARCHAR(36) PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('UNLOCK_ACCESS', 'RESET_DEVICE')),
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified BOOLEAN NOT NULL,
    consumed BOOLEAN NOT NULL
);
