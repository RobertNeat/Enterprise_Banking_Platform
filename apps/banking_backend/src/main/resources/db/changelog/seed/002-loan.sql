--liquibase formatted sql

--changeset robert_neat:seed-loan_application-001 context:mock-data
-- First 8 rows: loans.data.ts. LN-80180..LN-80099: stubs for loans referenced only by collections.data.ts.
INSERT INTO loan_application (id, customer_id, segment, product, stage, requested_amount,
                              approved_amount, score_result, dsti, bureau_inquiry_id,
                              interest_rate, term_months, created_at) VALUES
('LN-80231', 'CUST-001', 'retail', 'mortgage', 'contract', 480000.00, 460000.00, 782, 0.340, NULL,
 0.0790, 240, TIMESTAMP WITH TIME ZONE '2026-08-12 09:00:00+02:00'),
('LN-80244', 'CUST-002', 'retail', 'cash', 'disbursement', 35000.00, 35000.00, 701, 0.280, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-09-01 09:00:00+02:00'),
('LN-80255', 'CUST-003', 'business', 'investment', 'decision', 2200000.00, NULL, 655, 0.410, NULL,
 0.0790, 36, TIMESTAMP WITH TIME ZONE '2026-09-18 09:00:00+02:00'),
('LN-80261', 'CUST-004', 'retail', 'overdraft', 'schedule', 8000.00, 8000.00, 744, 0.220, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-06-02 09:00:00+02:00'),
('LN-80270', 'CUST-005', 'business', 'creditLine', 'scoring', 950000.00, NULL, 612, 0.470, NULL,
 0.0790, 36, TIMESTAMP WITH TIME ZONE '2026-09-29 09:00:00+02:00'),
('LN-80203', 'CUST-006', 'retail', 'cash', 'collections', 22000.00, 22000.00, 589, 0.520, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-03-15 09:00:00+01:00'),
('LN-80288', 'CUST-007', 'business', 'investment', 'application', 4100000.00, NULL, 0, 0.000, NULL,
 0.0790, 36, TIMESTAMP WITH TIME ZONE '2026-10-02 09:00:00+02:00'),
('LN-80219', 'CUST-008', 'retail', 'mortgage', 'affordability', 620000.00, NULL, 697, 0.390, NULL,
 0.0790, 240, TIMESTAMP WITH TIME ZONE '2026-09-20 09:00:00+02:00'),
('LN-80180', 'CUST-010', 'retail', 'cash', 'collections', 18000.00, 18000.00, 610, 0.450, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2025-11-10 09:00:00+01:00'),
('LN-80175', 'CUST-011', 'retail', 'cash', 'collections', 7000.00, 7000.00, 640, 0.380, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-01-20 09:00:00+01:00'),
('LN-80162', 'CUST-012', 'retail', 'overdraft', 'collections', 3000.00, 3000.00, 668, 0.310, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-03-02 09:00:00+01:00'),
('LN-80120', 'CUST-007', 'business', 'creditLine', 'collections', 60000.00, 60000.00, 702, 0.360,
 NULL, 0.0790, 36, TIMESTAMP WITH TIME ZONE '2025-12-01 09:00:00+01:00'),
('LN-80099', 'CUST-008', 'retail', 'cash', 'collections', 12000.00, 12000.00, 633, 0.420, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2025-10-15 09:00:00+02:00');

--changeset robert_neat:seed-loan_stage_transition-001 context:mock-data
INSERT INTO loan_stage_transition (loan_application_id, from_stage, to_stage, approval_id,
                                   transitioned_at, transitioned_by) VALUES
('LN-80231', 'application', 'scoring', NULL, TIMESTAMP WITH TIME ZONE '2026-08-13 10:00:00+02:00', 'PRC-10234'),
('LN-80231', 'scoring', 'affordability', NULL, TIMESTAMP WITH TIME ZONE '2026-08-18 11:30:00+02:00', 'PRC-20456'),
('LN-80231', 'affordability', 'decision', NULL, TIMESTAMP WITH TIME ZONE '2026-08-25 14:00:00+02:00', 'PRC-20456'),
('LN-80231', 'decision', 'contract', NULL, TIMESTAMP WITH TIME ZONE '2026-09-02 09:45:00+02:00', 'PRC-10234'),
('LN-80203', 'application', 'scoring', NULL, TIMESTAMP WITH TIME ZONE '2026-03-16 10:00:00+01:00', 'PRC-10234'),
('LN-80203', 'contract', 'disbursement', NULL, TIMESTAMP WITH TIME ZONE '2026-03-28 12:00:00+01:00', 'PRC-10234'),
('LN-80203', 'schedule', 'collections', NULL, TIMESTAMP WITH TIME ZONE '2026-07-06 08:30:00+02:00', 'PRC-30678');

--changeset robert_neat:seed-loan_stage_transition-002 context:mock-data
-- LN-80255's 'decision' transition is added here (new changeset, not editing -001 in place —
-- Liquibase checksums the already-run -001 on persistent Postgres deployments) with
-- governance_mock's seeded PENDING approval (22222222-2222-4222-8222-222222222002) so
-- advance-stage's live governance lookup resolves against a real seed, correctly 409ing until
-- that approval is decided.
INSERT INTO loan_stage_transition (loan_application_id, from_stage, to_stage, approval_id,
                                   transitioned_at, transitioned_by) VALUES
('LN-80255', 'scoring', 'decision', '22222222-2222-4222-8222-222222222002', TIMESTAMP WITH TIME ZONE '2026-09-18 12:00:00+02:00', 'PRC-20456');

--changeset robert_neat:seed-installment-001 context:mock-data
-- Partial proof-of-concept schedules (12 rows each, equal principal, declining interest at 7.9%).
-- LN-80231 has term 240; only its first 12 installments are seeded.
INSERT INTO installment (loan_application_id, installment_no, due_date, principal, interest, status) VALUES
('LN-80231', 1, DATE '2026-10-12', 1916.67, 3028.33, 'upcoming'),
('LN-80231', 2, DATE '2026-11-12', 1916.67, 3015.72, 'upcoming'),
('LN-80231', 3, DATE '2026-12-12', 1916.67, 3003.10, 'upcoming'),
('LN-80231', 4, DATE '2027-01-12', 1916.67, 2990.48, 'upcoming'),
('LN-80231', 5, DATE '2027-02-12', 1916.67, 2977.86, 'upcoming'),
('LN-80231', 6, DATE '2027-03-12', 1916.67, 2965.24, 'upcoming'),
('LN-80231', 7, DATE '2027-04-12', 1916.67, 2952.62, 'upcoming'),
('LN-80231', 8, DATE '2027-05-12', 1916.67, 2940.01, 'upcoming'),
('LN-80231', 9, DATE '2027-06-12', 1916.67, 2927.39, 'upcoming'),
('LN-80231', 10, DATE '2027-07-12', 1916.67, 2914.77, 'upcoming'),
('LN-80231', 11, DATE '2027-08-12', 1916.67, 2902.15, 'upcoming'),
('LN-80231', 12, DATE '2027-09-12', 1916.67, 2889.53, 'upcoming'),
('LN-80203', 1, DATE '2026-04-15', 1833.33, 144.83, 'paid'),
('LN-80203', 2, DATE '2026-05-15', 1833.33, 132.76, 'paid'),
('LN-80203', 3, DATE '2026-06-15', 1833.33, 120.69, 'paid'),
('LN-80203', 4, DATE '2026-07-15', 1833.33, 108.63, 'overdue'),
('LN-80203', 5, DATE '2026-08-15', 1833.33, 96.56, 'overdue'),
('LN-80203', 6, DATE '2026-09-15', 1833.33, 84.49, 'overdue'),
('LN-80203', 7, DATE '2026-10-15', 1833.33, 72.42, 'upcoming'),
('LN-80203', 8, DATE '2026-11-15', 1833.33, 60.35, 'upcoming'),
('LN-80203', 9, DATE '2026-12-15', 1833.33, 48.28, 'upcoming'),
('LN-80203', 10, DATE '2027-01-15', 1833.33, 36.21, 'upcoming'),
('LN-80203', 11, DATE '2027-02-15', 1833.33, 24.14, 'upcoming'),
('LN-80203', 12, DATE '2027-03-15', 1833.37, 12.07, 'upcoming');
