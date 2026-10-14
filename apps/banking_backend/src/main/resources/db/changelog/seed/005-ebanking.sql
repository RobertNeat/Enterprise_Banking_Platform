--liquibase formatted sql

--changeset robert_neat:seed-ebanking_user-001 context:mock-data
INSERT INTO ebanking_user (id, customer_id, username, channel, access_status, daily_limit,
                           last_login, permissions) VALUES
('EB-1001', 'CUST-001', 'j.wisniewski', 'mobile', 'locked', 10000.00,
 TIMESTAMP WITH TIME ZONE '2026-10-03 14:22:00+02:00', 'Przelewy,Historia,Lokaty'),
('EB-1002', 'CUST-002', 'k.zajac', 'web', 'active', 15000.00,
 TIMESTAMP WITH TIME ZONE '2026-10-05 08:05:00+02:00', 'Przelewy,Historia,Karty,Kredyty'),
('EB-1003', 'CUST-003', 'agrobud.admin', 'web', 'active', 200000.00,
 TIMESTAMP WITH TIME ZONE '2026-10-04 19:40:00+02:00', 'Przelewy masowe,Autoryzacje,Raporty'),
('EB-1004', 'CUST-004', 'p.kaczmarek', 'mobile', 'pendingReset', 5000.00,
 TIMESTAMP WITH TIME ZONE '2026-09-29 11:10:00+02:00', 'Przelewy,Historia'),
('EB-1005', 'CUST-006', 'm.lis', 'web', 'locked', 3000.00,
 TIMESTAMP WITH TIME ZONE '2026-09-18 16:52:00+02:00', 'Historia');
