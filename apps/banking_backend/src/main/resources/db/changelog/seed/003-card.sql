--liquibase formatted sql

--changeset robert_neat:seed-payment_card-001 context:mock-data
INSERT INTO payment_card (id, customer_id, masked_number, network, product, status, expiry,
                          limit_daily, limit_atm) VALUES
('CRD-5521', 'CUST-001', '4532 •••• •••• 0192', 'Visa', 'Visa Gold', 'active', '2028-05', 5000.00, 2000.00),
('CRD-5522', 'CUST-002', '5412 •••• •••• 7741', 'Mastercard', 'Mastercard World', 'blocked', '2027-11', 8000.00, 3000.00),
('CRD-5530', 'CUST-003', '4916 •••• •••• 3310', 'Visa', 'Visa Business', 'active', '2029-02', 20000.00, 5000.00),
('CRD-5541', 'CUST-004', '5577 •••• •••• 9087', 'Mastercard', 'Mastercard Standard', 'pending', '2029-06', 3000.00, 1500.00),
('CRD-5498', 'CUST-006', '4024 •••• •••• 4412', 'Visa', 'Visa Classic', 'expired', '2026-01', 2500.00, 1000.00);

--changeset robert_neat:seed-card_authorization_ref-001 context:mock-data
-- IDs are real UUID v4 (card_processor_mock contract: id is "string (UUID)" for /authorizations).
-- Amazon EU authorization (amount=1340.20) is approved per the contract rule (amount <= 5000 -> approved: true).
INSERT INTO card_authorization_ref (id, card_id, merchant, amount, occurred_at, processor, approved) VALUES
('dfd92a8d-976a-4601-97be-13ab2e09c7b1', 'CRD-5521', 'Empik Warszawa', 214.50, TIMESTAMP WITH TIME ZONE '2026-10-04 12:00:00+02:00', 'Visa', TRUE),
('fc936c4c-ffaf-4dd1-9fdd-c5b4af746a98', 'CRD-5521', 'Orlen Stacja 213', 180.00, TIMESTAMP WITH TIME ZONE '2026-10-03 12:00:00+02:00', 'Visa', TRUE),
('449084f7-77b5-4cab-a790-e680763a6572', 'CRD-5530', 'Amazon EU', 1340.20, TIMESTAMP WITH TIME ZONE '2026-10-02 12:00:00+02:00', 'Visa', TRUE),
('c78bf1a6-6d59-4a54-8de4-f5a3bc862310', 'CRD-5522', 'Biedronka', 96.40, TIMESTAMP WITH TIME ZONE '2026-09-28 12:00:00+02:00', 'Mastercard', TRUE);

--changeset robert_neat:seed-chargeback_ref-001 context:mock-data
-- Illustrative row (cards.data.ts has no chargebacks) proving the FK chain end to end.
-- reason_code=4837 (unauthorized transaction, card not present) fits an online purchase (Amazon EU).
INSERT INTO chargeback_ref (id, card_authorization_ref_id, reason_code, status, filed_at) VALUES
('d9a633a4-1223-48cd-b470-217a69610114', '449084f7-77b5-4cab-a790-e680763a6572', '4837', 'FILED', TIMESTAMP WITH TIME ZONE '2026-10-03 10:00:00+02:00');
