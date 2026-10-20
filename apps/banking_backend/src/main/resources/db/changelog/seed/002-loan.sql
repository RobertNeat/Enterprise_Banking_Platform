--liquibase formatted sql

--changeset robert_neat:seed-loan_application-001 context:mock-data
-- First 8 rows: loans.data.ts. LN-80180..LN-80099: stubs for loans referenced only by collections.data.ts.
-- LN-80231 and LN-80203 are the only loans with a seeded installment schedule (seed-installment-001
-- below) — their created_at (and the matching loan_stage_transition rows further down) are shifted
-- by the same relative offset as their installment schedule, so "loan created -> stage transitions
-- -> first installment due" still reads as a coherent timeline relative to CURRENT_DATE/TIMESTAMP at
-- seed time. The other loans here are historical/narrative only (no installment rows reference
-- them), so they keep their original fixed calendar literals — not in scope per the "remaining
-- time"/overdue UI logic this change targets.
INSERT INTO loan_application (id, customer_id, segment, product, stage, requested_amount,
                              approved_amount, score_result, dsti, bureau_inquiry_id,
                              interest_rate, term_months, created_at) VALUES
('LN-80231', 'CUST-001', 'retail', 'mortgage', 'contract', 480000.00, 460000.00, 782, 0.340, NULL,
 0.0790, 240, CURRENT_TIMESTAMP - INTERVAL '54' DAY),
('LN-80244', 'CUST-002', 'retail', 'cash', 'disbursement', 35000.00, 35000.00, 701, 0.280, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-09-01 09:00:00+02:00'),
('LN-80255', 'CUST-003', 'business', 'investment', 'decision', 2200000.00, NULL, 655, 0.410, NULL,
 0.0790, 36, TIMESTAMP WITH TIME ZONE '2026-09-18 09:00:00+02:00'),
('LN-80261', 'CUST-004', 'retail', 'overdraft', 'schedule', 8000.00, 8000.00, 744, 0.220, NULL,
 0.0790, 12, TIMESTAMP WITH TIME ZONE '2026-06-02 09:00:00+02:00'),
('LN-80270', 'CUST-005', 'business', 'creditLine', 'scoring', 950000.00, NULL, 612, 0.470, NULL,
 0.0790, 36, TIMESTAMP WITH TIME ZONE '2026-09-29 09:00:00+02:00'),
('LN-80203', 'CUST-006', 'retail', 'cash', 'collections', 22000.00, 22000.00, 589, 0.520, NULL,
 0.0790, 12, CURRENT_TIMESTAMP - INTERVAL '6' MONTH - INTERVAL '21' DAY),
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
-- LN-80231/LN-80203 transitions shifted by the same offset as their created_at above, to stay
-- coupled to the (now relative) installment schedule in seed-installment-001.
INSERT INTO loan_stage_transition (loan_application_id, from_stage, to_stage, approval_id,
                                   transitioned_at, transitioned_by) VALUES
('LN-80231', 'application', 'scoring', NULL, CURRENT_TIMESTAMP - INTERVAL '53' DAY, 'PRC-10234'),
('LN-80231', 'scoring', 'affordability', NULL, CURRENT_TIMESTAMP - INTERVAL '48' DAY, 'PRC-20456'),
('LN-80231', 'affordability', 'decision', NULL, CURRENT_TIMESTAMP - INTERVAL '41' DAY, 'PRC-20456'),
('LN-80231', 'decision', 'contract', NULL, CURRENT_TIMESTAMP - INTERVAL '33' DAY, 'PRC-10234'),
('LN-80203', 'application', 'scoring', NULL, CURRENT_TIMESTAMP - INTERVAL '6' MONTH - INTERVAL '20' DAY, 'PRC-10234'),
('LN-80203', 'contract', 'disbursement', NULL, CURRENT_TIMESTAMP - INTERVAL '6' MONTH - INTERVAL '8' DAY, 'PRC-10234'),
('LN-80203', 'schedule', 'collections', NULL, CURRENT_TIMESTAMP - INTERVAL '3' MONTH + INTERVAL '2' DAY, 'PRC-30678');

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
-- due_date is relative to CURRENT_DATE at seed time (old fixed anchor was 2026-10-05), stepped by
-- whole months so the 'paid'/'overdue'/'upcoming' status column (not recomputed at runtime) stays
-- consistent with "now" regardless of when the volume is created: LN-80203's first 3 installments
-- remain months in the past (paid), the next 3 remain past-due (overdue), the rest remain upcoming.
INSERT INTO installment (loan_application_id, installment_no, due_date, principal, interest, status) VALUES
('LN-80231', 1, CAST(CURRENT_DATE + INTERVAL '7' DAY AS DATE), 1916.67, 3028.33, 'upcoming'),
('LN-80231', 2, CAST(CURRENT_DATE + INTERVAL '1' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 3015.72, 'upcoming'),
('LN-80231', 3, CAST(CURRENT_DATE + INTERVAL '2' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 3003.10, 'upcoming'),
('LN-80231', 4, CAST(CURRENT_DATE + INTERVAL '3' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2990.48, 'upcoming'),
('LN-80231', 5, CAST(CURRENT_DATE + INTERVAL '4' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2977.86, 'upcoming'),
('LN-80231', 6, CAST(CURRENT_DATE + INTERVAL '5' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2965.24, 'upcoming'),
('LN-80231', 7, CAST(CURRENT_DATE + INTERVAL '6' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2952.62, 'upcoming'),
('LN-80231', 8, CAST(CURRENT_DATE + INTERVAL '7' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2940.01, 'upcoming'),
('LN-80231', 9, CAST(CURRENT_DATE + INTERVAL '8' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2927.39, 'upcoming'),
('LN-80231', 10, CAST(CURRENT_DATE + INTERVAL '9' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2914.77, 'upcoming'),
('LN-80231', 11, CAST(CURRENT_DATE + INTERVAL '10' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2902.15, 'upcoming'),
('LN-80231', 12, CAST(CURRENT_DATE + INTERVAL '11' MONTH + INTERVAL '7' DAY AS DATE), 1916.67, 2889.53, 'upcoming'),
('LN-80203', 1, CAST(CURRENT_DATE - INTERVAL '6' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 144.83, 'paid'),
('LN-80203', 2, CAST(CURRENT_DATE - INTERVAL '5' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 132.76, 'paid'),
('LN-80203', 3, CAST(CURRENT_DATE - INTERVAL '4' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 120.69, 'paid'),
('LN-80203', 4, CAST(CURRENT_DATE - INTERVAL '3' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 108.63, 'overdue'),
('LN-80203', 5, CAST(CURRENT_DATE - INTERVAL '2' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 96.56, 'overdue'),
('LN-80203', 6, CAST(CURRENT_DATE - INTERVAL '1' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 84.49, 'overdue'),
('LN-80203', 7, CAST(CURRENT_DATE + INTERVAL '10' DAY AS DATE), 1833.33, 72.42, 'upcoming'),
('LN-80203', 8, CAST(CURRENT_DATE + INTERVAL '1' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 60.35, 'upcoming'),
('LN-80203', 9, CAST(CURRENT_DATE + INTERVAL '2' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 48.28, 'upcoming'),
('LN-80203', 10, CAST(CURRENT_DATE + INTERVAL '3' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 36.21, 'upcoming'),
('LN-80203', 11, CAST(CURRENT_DATE + INTERVAL '4' MONTH + INTERVAL '10' DAY AS DATE), 1833.33, 24.14, 'upcoming'),
('LN-80203', 12, CAST(CURRENT_DATE + INTERVAL '5' MONTH + INTERVAL '10' DAY AS DATE), 1833.37, 12.07, 'upcoming');
