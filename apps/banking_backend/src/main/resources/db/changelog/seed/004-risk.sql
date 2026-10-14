--liquibase formatted sql

--changeset robert_neat:seed-collateral_entry-001 context:mock-data
-- provision = valuation * ltv (exposure) * 1%/5%/25% per stage1/stage2/stage3 (credit_risk_mock /ecl/classify rule).
INSERT INTO collateral_entry (id, customer_id, type, valuation, ltv, rating, stage, provision,
                              ecl_classified_at) VALUES
('COL-3301', 'CUST-001', 'mortgage', 620000.00, 0.74, 'BBB', 'stage1', 4588.00, TIMESTAMP WITH TIME ZONE '2026-10-05 09:00:00+02:00'),
('COL-3302', 'CUST-003', 'guarantee', 2500000.00, 0.88, 'BB-', 'stage2', 110000.00, TIMESTAMP WITH TIME ZONE '2026-10-05 09:00:00+02:00'),
('COL-3303', 'CUST-005', 'pledge', 1100000.00, 0.65, 'A-', 'stage1', 7150.00, TIMESTAMP WITH TIME ZONE '2026-10-05 09:00:00+02:00'),
('COL-3304', 'CUST-006', 'mortgage', 410000.00, 1.02, 'CCC', 'stage3', 104550.00, TIMESTAMP WITH TIME ZONE '2026-10-05 09:00:00+02:00'),
('COL-3305', 'CUST-007', 'guarantee', 3800000.00, 0.59, 'A', 'stage1', 22420.00, TIMESTAMP WITH TIME ZONE '2026-10-05 09:00:00+02:00');
