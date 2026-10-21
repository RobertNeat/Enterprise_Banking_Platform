--liquibase formatted sql

--changeset robert_neat:fatca_crs_export-001
-- xml is a plain TEXT column (not a Postgres "oid" large object -- @Lob on this entity would map
-- to that instead and fail ddl-auto=validate against TEXT; same unbounded-text approach as
-- governance.approval_request.payload).
CREATE TABLE gateway.fatca_crs_export (
    report_id VARCHAR(50) PRIMARY KEY,
    period VARCHAR(20) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    xml TEXT NOT NULL
);
