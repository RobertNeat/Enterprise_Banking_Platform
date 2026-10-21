--liquibase formatted sql

--changeset robert_neat:seed-employee_account-001 context:mock-data
-- Demo accounts previously seeded in AuthService's constructor (seedDemoAccounts()/register()).
-- Fixed ids (not random, matching the old in-memory ids) so docs/tests can reference them directly.
-- role strings must match RolePermissions' constants exactly (ADMIN, LOANS, RISK, COLLECTIONS,
-- CONTACT, COMPLIANCE) - permissions are derived from role, not stored.
INSERT INTO identity_access.employee_account (id, employee_id, password, role, status) VALUES
('admin', 'PRC-00001', 'Admin12345', 'Administrator systemu', 'ACTIVE'),
('loans', 'PRC-10234', 'Loans12345', 'Doradca kredytowy', 'ACTIVE'),
('risk', 'PRC-20456', 'Risk123456', 'Analityk ryzyka', 'ACTIVE'),
('collections', 'PRC-30678', 'Collect1234', 'Windykator', 'ACTIVE'),
('contact', 'PRC-40890', 'Contact1234', 'Konsultant kanału kontaktu', 'ACTIVE'),
('compliance', 'PRC-50123', 'Compliance1', 'Compliance / AML', 'ACTIVE');
