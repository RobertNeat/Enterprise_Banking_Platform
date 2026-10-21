--liquibase formatted sql

--changeset robert_neat:audit_event-001
CREATE TABLE governance.audit_event (
    id VARCHAR(36) PRIMARY KEY,
    actor VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    details VARCHAR
);

--changeset robert_neat:audit_event-002
-- Only the table is schema-qualified: Postgres rejects a schema-qualified index name and always
-- places the index in its table's schema anyway.
CREATE INDEX ix_audit_event_entity_type_id ON governance.audit_event (entity_type, entity_id);
