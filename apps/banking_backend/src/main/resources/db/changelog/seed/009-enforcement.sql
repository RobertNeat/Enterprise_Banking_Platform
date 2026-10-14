--liquibase formatted sql

--changeset robert_neat:seed-enforcement_case-001 context:mock-data
-- kind defaults to NON_ALIMONY: mock data carries no alimony signal.
INSERT INTO enforcement_case (id, gateway_enforcement_id, customer_id, bank_account_id, bailiff,
                              case_number, kind, blocked_amount, transferred_amount,
                              protected_amount, status, approval_id) VALUES
('EN-2201', NULL, 'CUST-006', 'ACC-0006', 'Komornik Sądowy A. Dąbrowski', 'KM 1123/26', 'NON_ALIMONY',
 18400.00, 6200.00, 1631.40, 'active', NULL),
('EN-2198', NULL, 'CUST-004', 'ACC-0004', 'Komornik Sądowy M. Wójcik', 'KM 998/26', 'NON_ALIMONY',
 4200.00, 4200.00, 1631.40, 'settled', NULL),
('EN-2190', NULL, 'CUST-005', 'ACC-0005', 'Komornik Sądowy T. Nowicki', 'KM 887/25', 'NON_ALIMONY',
 128000.00, 128000.00, 0.00, 'closed', NULL),
('EN-2205', NULL, 'CUST-002', 'ACC-0002', 'Komornik Sądowy A. Dąbrowski', 'KM 1145/26', 'NON_ALIMONY',
 0.00, NULL, 1631.40, 'registered', NULL);
