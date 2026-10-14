--liquibase formatted sql

--changeset robert_neat:seed-customer-001 context:mock-data
INSERT INTO customer (id, full_name, segment, tax_id, risk_rating) VALUES
('CUST-001', 'Jan Wiśniewski', 'retail', 'PL482913057', 'BBB'),
('CUST-002', 'Katarzyna Zając', 'retail', 'PL731508264', 'BBB'),
('CUST-003', 'Agro-Bud S.A.', 'business', 'PL905172638', 'BB-'),
('CUST-004', 'Piotr Kaczmarek', 'retail', 'PL264819375', 'BBB'),
('CUST-005', 'Nowak Logistyka Sp. z o.o.', 'business', 'PL617340982', 'A-'),
('CUST-006', 'Maria Lis', 'retail', 'PL358027146', 'CCC'),
('CUST-007', 'BetaTech S.A.', 'business', 'PL842695013', 'A'),
('CUST-008', 'Anna Kowalska', 'retail', 'PL170583429', 'BBB'),
('CUST-009', 'Nowak Sp. z o.o.', 'business', 'PL593614870', 'BB'),
('CUST-010', 'Robert Nowak', 'retail', 'PL426087531', 'B'),
('CUST-011', 'Ewa Majewska', 'retail', 'PL789352614', 'BB'),
('CUST-012', 'Tomasz Kowal', 'retail', 'PL035968247', 'BB');

--changeset robert_neat:seed-employee-001 context:mock-data
-- DEMO_ACCOUNTS (login.page.ts) + owners of customer cases (cases.data.ts). No passwords here.
INSERT INTO employee (id, first_name, last_name, email, phone, department, branch, position,
                      supervisor_id, role, business_lines, start_date, status) VALUES
('PRC-00001', 'Krzysztof', 'Malinowski', 'krzysztof.malinowski@bank.example.pl', '+48 22 500 10 01',
 'IT', 'Centrala Warszawa', 'Administrator systemu', NULL, 'Administrator systemu', 'it',
 DATE '2019-03-01', 'ACTIVE'),
('PRC-10234', 'Magdalena', 'Wróbel', 'magdalena.wrobel@bank.example.pl', '+48 22 500 10 34',
 'Kredyty detaliczne', 'Oddział Warszawa Centrum', 'Doradca kredytowy', NULL, 'Doradca kredytowy',
 'retail', DATE '2021-06-14', 'ACTIVE'),
('PRC-20456', 'Paweł', 'Szymański', 'pawel.szymanski@bank.example.pl', '+48 22 500 20 56',
 'Ryzyko kredytowe', 'Centrala Warszawa', 'Analityk ryzyka', NULL, 'Analityk ryzyka', 'risk',
 DATE '2020-09-01', 'ACTIVE'),
('PRC-30678', 'Joanna', 'Dudek', 'joanna.dudek@bank.example.pl', '+48 42 500 30 78',
 'Windykacja', 'Centrala Łódź', 'Windykator', NULL, 'Windykator', 'collections',
 DATE '2022-01-10', 'ACTIVE'),
('PRC-40890', 'Michał', 'Grabowski', 'michal.grabowski@bank.example.pl', '+48 12 500 40 90',
 'Contact Center', 'Centrala Kraków', 'Konsultant kanału kontaktu', NULL,
 'Konsultant kanału kontaktu', 'retail', DATE '2023-04-03', 'ACTIVE'),
('PRC-50123', 'Agnieszka', 'Pawlak', 'agnieszka.pawlak@bank.example.pl', '+48 22 500 51 23',
 'Compliance', 'Centrala Warszawa', 'Specjalista Compliance / AML', NULL, 'Compliance / AML',
 'compliance', DATE '2018-11-19', 'ACTIVE'),
('PRC-60001', 'Anna', 'Kowalska', 'anna.kowalska@bank.example.pl', '+48 22 500 60 01',
 'Obsługa klienta', 'Oddział Warszawa Centrum', 'Doradca klienta', NULL, 'Doradca klienta',
 'retail,corporate', DATE '2020-02-03', 'ACTIVE'),
('PRC-60002', 'Piotr', 'Zieliński', 'piotr.zielinski@bank.example.pl', '+48 22 500 60 02',
 'Obsługa klienta', 'Oddział Warszawa Mokotów', 'Doradca klienta', NULL, 'Doradca klienta',
 'retail', DATE '2021-09-06', 'ACTIVE'),
('PRC-60003', 'Marek', 'Lis', 'marek.lis@bank.example.pl', '+48 61 500 60 03',
 'Obsługa klienta', 'Oddział Poznań', 'Doradca klienta', NULL, 'Doradca klienta', 'corporate',
 DATE '2022-05-16', 'ACTIVE');

--changeset robert_neat:seed-bank_account-001 context:mock-data
-- blocked_amount > 0 only where enforcement.data.ts has a blockedAmount for that customer.
INSERT INTO bank_account (id, customer_id, masked_number, balance, blocked_amount) VALUES
('ACC-0001', 'CUST-001', 'PL61 •••• •••• •••• 1234', 48250.00, 0.00),
('ACC-0002', 'CUST-002', 'PL27 •••• •••• •••• 5871', 12940.55, 0.00),
('ACC-0003', 'CUST-003', 'PL83 •••• •••• •••• 2046', 1845300.00, 0.00),
('ACC-0004', 'CUST-004', 'PL14 •••• •••• •••• 7390', 6120.80, 0.00),
('ACC-0005', 'CUST-005', 'PL52 •••• •••• •••• 6613', 412870.00, 0.00),
('ACC-0006', 'CUST-006', 'PL09 •••• •••• •••• 3158', 21033.40, 18400.00),
('ACC-0007', 'CUST-007', 'PL70 •••• •••• •••• 9402', 2960400.00, 0.00),
('ACC-0008', 'CUST-008', 'PL36 •••• •••• •••• 4725', 18760.15, 0.00),
('ACC-0009', 'CUST-009', 'PL45 •••• •••• •••• 8094', 287540.00, 0.00),
('ACC-0010', 'CUST-010', 'PL98 •••• •••• •••• 1567', 940.20, 0.00),
('ACC-0011', 'CUST-011', 'PL21 •••• •••• •••• 6380', 2315.60, 0.00),
('ACC-0012', 'CUST-012', 'PL67 •••• •••• •••• 5219', 4108.90, 0.00);
