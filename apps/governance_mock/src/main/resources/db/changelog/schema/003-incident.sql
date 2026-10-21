--liquibase formatted sql

--changeset robert_neat:ict_incident-001
CREATE TABLE governance.ict_incident (
    id VARCHAR(36) PRIMARY KEY,
    source VARCHAR(100) NOT NULL,
    description VARCHAR NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);
