--liquibase formatted sql

--changeset robert_neat:seed-customer_case-001 context:mock-data
-- "now" anchor = 2026-10-05 12:00 (+02:00); sla_due_at = now + slaRemainingHours from cases.data.ts,
-- registered_at = statutory_deadline - 30 days.
INSERT INTO customer_case (id, customer_id, subject, owner_id, priority, case_type, registered_at,
                           statutory_deadline, sla_due_at, status, escalated_at, escalation_source,
                           governance_audit_event_id) VALUES
('CS-10231', 'CUST-009', 'Reklamacja opłaty za prowadzenie konta', 'PRC-60001', 'high', 'complaint',
 TIMESTAMP WITH TIME ZONE '2026-09-07 09:00:00+02:00', DATE '2026-10-07',
 TIMESTAMP WITH TIME ZONE '2026-10-06 06:00:00+02:00', 'escalated',
 TIMESTAMP WITH TIME ZONE '2026-10-05 08:00:00+02:00', 'MANUAL', NULL),
('CS-10229', 'CUST-001', 'Wniosek o restrukturyzację kredytu', 'PRC-60002', 'high',
 'restructuring_request', TIMESTAMP WITH TIME ZONE '2026-09-12 09:00:00+02:00', DATE '2026-10-12',
 TIMESTAMP WITH TIME ZONE '2026-10-07 16:00:00+02:00', 'inProgress', NULL, NULL, NULL),
('CS-10225', 'CUST-003', 'Zapytanie o status wniosku kredytowego', 'PRC-60003', 'medium', 'inquiry',
 TIMESTAMP WITH TIME ZONE '2026-09-18 09:00:00+02:00', DATE '2026-10-18',
 TIMESTAMP WITH TIME ZONE '2026-10-09 12:00:00+02:00', 'new', NULL, NULL, NULL),
('CS-10219', 'CUST-002', 'Reklamacja transakcji kartowej', 'PRC-60001', 'medium', 'complaint',
 TIMESTAMP WITH TIME ZONE '2026-09-02 09:00:00+02:00', DATE '2026-10-02',
 TIMESTAMP WITH TIME ZONE '2026-10-05 12:00:00+02:00', 'resolved', NULL, NULL, NULL),
('CS-10233', 'CUST-006', 'Zgłoszenie błędnej blokady środków', 'PRC-60002', 'high', 'complaint',
 TIMESTAMP WITH TIME ZONE '2026-09-06 09:00:00+02:00', DATE '2026-10-06',
 TIMESTAMP WITH TIME ZONE '2026-10-05 18:00:00+02:00', 'escalated',
 TIMESTAMP WITH TIME ZONE '2026-10-05 10:00:00+02:00', 'MANUAL', NULL);
