--liquibase formatted sql

--changeset robert_neat:seed-approval_request-001 context:mock-data
-- Seeds approvals for loan applications from banking_backend's loan_application /
-- loan_stage_transition seed (db/changelog/seed/002-loan.sql) so the maker-checker demo has
-- data consistent with the backend's state, rather than starting from an empty queue.
-- Fixed ids (not random) so docs/tests can reference them directly.
--
-- LN-80231 already transitioned decision -> contract in the backend seed (approved by
-- PRC-10234, the loan advisor who also requested/closed the loan) — seeded here already
-- APPROVED, decided by PRC-00001 (admin, a different employee than the requester, satisfying
-- governance_mock's own four-eyes check) to keep an audit trail consistent with "decided by
-- someone other than the maker".
--
-- LN-80255 is still sitting in stage='decision' in the backend seed, linked via its
-- loan_stage_transition.approval_id (db/changelog/seed/002-loan.sql) — seeded here as PENDING,
-- requested by PRC-20456 (the credit risk analyst who scored it), so the approvals queue demo
-- has something left to act on.
--
-- EN-2201 is 'active' in the backend's enforcement_case seed (db/changelog/seed/009-enforcement.sql),
-- linked via its approval_id — seeded here already APPROVED (decided by PRC-00001, admin) so
-- "Wykonaj transfer" on this seeded case can succeed without first requesting a new approval.
INSERT INTO governance.approval_request (id, type, requested_by, payload, status, created_at, decided_by, decided_at) VALUES
('11111111-1111-4111-8111-111111111001', 'LOAN_STAGE_TRANSITION', 'PRC-10234',
 'LN-80231: affordability -> decision -> contract (480000.00 PLN, retail mortgage)',
 'APPROVED', TIMESTAMP WITH TIME ZONE '2026-09-02 06:45:00+00', 'PRC-00001',
 TIMESTAMP WITH TIME ZONE '2026-09-02 07:45:00+00'),
('22222222-2222-4222-8222-222222222002', 'LOAN_STAGE_TRANSITION', 'PRC-20456',
 'LN-80255: scoring -> decision (2200000.00 PLN, business investment, score 655)',
 'PENDING', CURRENT_TIMESTAMP, NULL, NULL),
('44444444-4444-4444-8444-444444444001', 'ENFORCEMENT_TRANSFER', 'PRC-30678',
 'Enforcement case EN-2201 blocked amount 18400.00',
 'APPROVED', TIMESTAMP WITH TIME ZONE '2026-07-15 09:00:00+00', 'PRC-00001',
 TIMESTAMP WITH TIME ZONE '2026-07-15 10:00:00+00');

--changeset robert_neat:seed-approval_request-002 context:mock-data
-- Audit trail entries for the two seeded decided approvals above, matching the ApprovalService
-- decide() behaviour (one APPROVAL_APPROVED audit event per decided approval).
INSERT INTO governance.audit_event (id, actor, action, entity_type, entity_id, timestamp, details) VALUES
('33333333-3333-4333-8333-333333333001', 'PRC-00001', 'APPROVAL_APPROVED', 'APPROVAL',
 '11111111-1111-4111-8111-111111111001', TIMESTAMP WITH TIME ZONE '2026-09-02 07:45:00+00',
 'type=LOAN_STAGE_TRANSITION; requestedBy=PRC-10234; payload=LN-80231: affordability -> decision -> contract (480000.00 PLN, retail mortgage)'),
('55555555-5555-4555-8555-555555555001', 'PRC-00001', 'APPROVAL_APPROVED', 'APPROVAL',
 '44444444-4444-4444-8444-444444444001', TIMESTAMP WITH TIME ZONE '2026-07-15 10:00:00+00',
 'type=ENFORCEMENT_TRANSFER; requestedBy=PRC-30678; payload=Enforcement case EN-2201 blocked amount 18400.00');
