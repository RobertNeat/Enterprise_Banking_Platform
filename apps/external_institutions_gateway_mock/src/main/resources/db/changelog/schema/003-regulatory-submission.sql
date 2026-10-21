--liquibase formatted sql

--changeset robert_neat:regulatory_submission-001
CREATE TABLE gateway.regulatory_submission (
    submission_id VARCHAR(36) PRIMARY KEY,
    report_id VARCHAR(50) NOT NULL,
    recipient VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    period VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

--changeset robert_neat:regulatory_submission-002
-- @ElementCollection table for RegulatorySubmission.validationErrors (List<String>) -- one row per
-- error message, ordered by error_order (matches @OrderColumn on the entity).
CREATE TABLE gateway.regulatory_submission_error (
    submission_id VARCHAR(36) NOT NULL REFERENCES gateway.regulatory_submission (submission_id),
    error_order INTEGER NOT NULL,
    error_message VARCHAR NOT NULL,
    PRIMARY KEY (submission_id, error_order)
);
