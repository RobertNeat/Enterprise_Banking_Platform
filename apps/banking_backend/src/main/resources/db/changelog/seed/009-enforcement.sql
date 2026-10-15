--liquibase formatted sql

--changeset robert_neat:seed-enforcement_case-001 context:mock-data
-- kind defaults to NON_ALIMONY: mock data carries no alimony signal.
-- gateway_enforcement_id values match external_institutions_gateway_mock's EnforcementService.seed()
-- fixed UUIDs (33333333-...-001..004), so transfer()'s live gateway call resolves against seeded
-- cases, not just newly registered ones. EN-2201's approval_id matches governance_mock's seeded
-- APPROVED ENFORCEMENT_TRANSFER approval (44444444-4444-4444-8444-444444444001).
INSERT INTO enforcement_case (id, gateway_enforcement_id, customer_id, bank_account_id, bailiff,
                              case_number, kind, blocked_amount, transferred_amount,
                              protected_amount, status, approval_id) VALUES
('EN-2201', '33333333-3333-4333-8333-333333333001', 'CUST-006', 'ACC-0006', 'Komornik Sądowy A. Dąbrowski', 'KM 1123/26', 'NON_ALIMONY',
 18400.00, 6200.00, 1631.40, 'active', '44444444-4444-4444-8444-444444444001'),
('EN-2198', '33333333-3333-4333-8333-333333333002', 'CUST-004', 'ACC-0004', 'Komornik Sądowy M. Wójcik', 'KM 998/26', 'NON_ALIMONY',
 4200.00, 4200.00, 1631.40, 'settled', NULL),
('EN-2190', '33333333-3333-4333-8333-333333333003', 'CUST-005', 'ACC-0005', 'Komornik Sądowy T. Nowicki', 'KM 887/25', 'NON_ALIMONY',
 128000.00, 128000.00, 0.00, 'closed', NULL),
('EN-2205', '33333333-3333-4333-8333-333333333004', 'CUST-002', 'ACC-0002', 'Komornik Sądowy A. Dąbrowski', 'KM 1145/26', 'NON_ALIMONY',
 0.00, NULL, 1631.40, 'registered', NULL);
