--liquibase formatted sql

--changeset robert_neat:customer_case-001
CREATE TABLE customer_case (
    id VARCHAR(10) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    subject VARCHAR(300) NOT NULL,
    owner_id VARCHAR(10) NOT NULL REFERENCES employee (id),
    priority VARCHAR(10) NOT NULL CHECK (priority IN ('low', 'medium', 'high')),
    case_type VARCHAR(30) NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    statutory_deadline DATE NOT NULL,
    sla_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(15) NOT NULL CHECK (status IN ('new', 'inProgress', 'escalated', 'resolved')),
    escalated_at TIMESTAMP WITH TIME ZONE,
    escalation_source VARCHAR(10) CHECK (escalation_source IN ('MANUAL', 'AUTO_SLA')),
    governance_audit_event_id VARCHAR(36)
);

--changeset robert_neat:customer_case-002
-- case_type previously had no CHECK (banking_backend.md item 17) — restricted to the same list the
-- UI offers (banking_web cases.page.ts CASE_TYPES) and that CaseActionsService now validates in
-- Java before insert, so an invalid value is rejected as 400 rather than surfacing as a raw
-- DataIntegrityViolationException (500) from this constraint.
ALTER TABLE customer_case ADD CONSTRAINT ck_customer_case_case_type
    CHECK (case_type IN ('complaint', 'restructuring_request', 'inquiry'));
