--liquibase formatted sql

--changeset robert_neat:seed-collections_case-001 context:mock-data
INSERT INTO collections_case (id, loan_application_id, days_past_due, outstanding_amount, stage) VALUES
('DBT-6601', 'LN-80203', 92, 22600.00, 'legal'),
('DBT-6598', 'LN-80180', 61, 14200.00, 'external'),
('DBT-6590', 'LN-80175', 34, 5400.00, 'settlement'),
('DBT-6587', 'LN-80162', 15, 2100.00, 'reminder'),
('DBT-6580', 'LN-80120', 7, 48900.00, 'monitoring'),
('DBT-6572', 'LN-80099', 48, 9800.00, 'restructuring');

--changeset robert_neat:seed-collections_action-001 context:mock-data
-- DBT-6601's two actions are shifted by the same relative offset as LN-80203's installment schedule
-- (seed/002-loan.sql) — both stay shortly after that loan's first overdue installment, regardless of
-- when the volume is created. DBT-6598 has no linked installment schedule (LN-80180 is a stub loan),
-- so it keeps its original fixed calendar literal — narrative only, not in scope.
INSERT INTO collections_action (collections_case_id, action_type, performed_at, performed_by,
                                governance_audit_event_id) VALUES
('DBT-6601', 'REMINDER_SENT', CURRENT_TIMESTAMP - INTERVAL '3' MONTH + INTERVAL '15' DAY, 'PRC-30678', NULL),
('DBT-6601', 'SETTLEMENT_PROPOSED', CURRENT_TIMESTAMP - INTERVAL '2' MONTH + INTERVAL '21' DAY, 'PRC-30678', NULL),
('DBT-6598', 'EXTERNAL_TRANSFER', TIMESTAMP WITH TIME ZONE '2026-09-30 14:30:00+02:00', 'PRC-30678', NULL);
