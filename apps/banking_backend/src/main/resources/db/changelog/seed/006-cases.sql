--liquibase formatted sql

--changeset robert_neat:seed-customer_case-001 context:mock-data
-- Dates are relative to CURRENT_DATE/CURRENT_TIMESTAMP at seed time (not a fixed calendar anchor),
-- so "SLA remaining"/overdue scenarios stay meaningful no matter when the volume is created.
-- Each row keeps the same relative offsets the old fixed-anchor seed used (anchor was 2026-10-05
-- 12:00+02:00): registered_at = statutory_deadline - 30 days. Two deliberate exceptions to the old
-- offsets, both overdue-by-design:
--  - CS-10231/CS-10233 (status='escalated'): sla_due_at and escalated_at both moved into the past
--    (escalated_at still earlier than sla_due_at, preserving the MANUAL invariant from
--    docs/services/banking_backend.md item 1) so escalated seed rows actually read as past-SLA.
--  - CS-10229 (status='inProgress'): sla_due_at moved to now - 4h (overdue) while its
--    statutory_deadline stays in the future (anchor + 7d), so at least one non-terminal seeded case
--    can exercise the AUTO_SLA auto-escalation path (governance_mock escalates when
--    slaRemainingHours <= 0) without also being past its legal deadline.
INSERT INTO customer_case (id, customer_id, subject, owner_id, priority, case_type, registered_at,
                           statutory_deadline, sla_due_at, status, escalated_at, escalation_source,
                           governance_audit_event_id) VALUES
('CS-10231', 'CUST-009', 'Reklamacja opłaty za prowadzenie konta', 'PRC-60001', 'high', 'complaint',
 CURRENT_TIMESTAMP - INTERVAL '28' DAY - INTERVAL '3' HOUR, CAST(CURRENT_DATE + 2 AS DATE),
 CURRENT_TIMESTAMP - INTERVAL '12' HOUR, 'escalated',
 CURRENT_TIMESTAMP - INTERVAL '30' HOUR, 'MANUAL', NULL),
('CS-10229', 'CUST-001', 'Wniosek o restrukturyzację kredytu', 'PRC-60002', 'high',
 'restructuring_request', CURRENT_TIMESTAMP - INTERVAL '23' DAY - INTERVAL '3' HOUR,
 CAST(CURRENT_DATE + 7 AS DATE),
 CURRENT_TIMESTAMP - INTERVAL '4' HOUR, 'inProgress', NULL, NULL, NULL),
('CS-10225', 'CUST-003', 'Zapytanie o status wniosku kredytowego', 'PRC-60003', 'medium', 'inquiry',
 CURRENT_TIMESTAMP - INTERVAL '17' DAY - INTERVAL '3' HOUR, CAST(CURRENT_DATE + 13 AS DATE),
 CURRENT_TIMESTAMP + INTERVAL '4' DAY, 'new', NULL, NULL, NULL),
('CS-10219', 'CUST-002', 'Reklamacja transakcji kartowej', 'PRC-60001', 'medium', 'complaint',
 CURRENT_TIMESTAMP - INTERVAL '33' DAY - INTERVAL '3' HOUR, CAST(CURRENT_DATE - 3 AS DATE),
 CURRENT_TIMESTAMP, 'resolved', NULL, NULL, NULL),
('CS-10233', 'CUST-006', 'Zgłoszenie błędnej blokady środków', 'PRC-60002', 'high', 'complaint',
 CURRENT_TIMESTAMP - INTERVAL '29' DAY - INTERVAL '3' HOUR, CAST(CURRENT_DATE + 1 AS DATE),
 CURRENT_TIMESTAMP - INTERVAL '6' HOUR, 'escalated',
 CURRENT_TIMESTAMP - INTERVAL '8' HOUR, 'MANUAL', NULL);
