--liquibase formatted sql

--changeset robert_neat:seed-enforcement_case-001 context:mock-data
-- Fixed gateway-side ids matching apps/banking_backend's enforcement_case seed
-- (db/changelog/seed/009-enforcement.sql) by case/customer -- the backend's own
-- enforcement_case.gateway_enforcement_id column is populated with these same ids, so demo
-- lookups resolve correctly without creating a new case. Previously seeded in
-- EnforcementService's @PostConstruct seed() method.
INSERT INTO gateway.enforcement_case
    (id, customer, bailiff, case_number, blocked_amount, transferred_amount, protected_amount, status, created_at, settled_at)
VALUES
    -- EN-2201: active, partially settled in reality but transferred_amount is NULL here (matches
    -- the old in-memory seed exactly -- no transfer() call has been made against this case yet).
    ('33333333-3333-4333-8333-333333333001', 'CUST-006', 'Komornik Sądowy A. Dąbrowski', 'KM 1123/26',
     18400.00, NULL, 1631.40, 'ACTIVE', '2026-07-10T09:00:00Z', NULL),
    -- EN-2198: fully settled (blocked == transferred).
    ('33333333-3333-4333-8333-333333333002', 'CUST-004', 'Komornik Sądowy M. Wójcik', 'KM 998/26',
     4200.00, 4200.00, 1631.40, 'SETTLED', '2026-05-02T09:00:00Z', '2026-05-20T12:00:00Z'),
    -- EN-2190: closed (fully transferred, case formally closed afterwards).
    ('33333333-3333-4333-8333-333333333003', 'CUST-005', 'Komornik Sądowy T. Nowicki', 'KM 887/25',
     128000.00, 128000.00, 0.00, 'CLOSED', '2025-11-15T09:00:00Z', '2025-12-01T09:00:00Z'),
    -- EN-2205: just registered, nothing blocked/transferred yet.
    ('33333333-3333-4333-8333-333333333004', 'CUST-002', 'Komornik Sądowy A. Dąbrowski', 'KM 1145/26',
     0.00, NULL, 1631.40, 'REGISTERED', '2026-10-01T09:00:00Z', NULL);
