--liquibase formatted sql

--changeset robert_neat:employee_account-001
CREATE TABLE identity_access.employee_account (
    id VARCHAR(50) PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL')),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(150)
);
