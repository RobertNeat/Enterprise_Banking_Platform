--liquibase formatted sql

--changeset robert_neat:auth_session-001
-- token -> employeeId association, replacing the old sessionsByToken in-memory map. employee_id is
-- a plain FK-like column (REFERENCES enforced here, not a JPA @ManyToOne — open-in-view is false
-- and nothing needs to lazy-load the account from a session).
CREATE TABLE identity_access.auth_session (
    token VARCHAR(36) PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL REFERENCES identity_access.employee_account (employee_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
