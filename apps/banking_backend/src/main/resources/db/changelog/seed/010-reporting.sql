--liquibase formatted sql

--changeset robert_neat:seed-regulatory_report-001 context:mock-data
INSERT INTO regulatory_report (id, recipient, name, period, due_date, status) VALUES
('RPT-KNF-001', 'KNF', 'Sprawozdanie adekwatności kapitałowej', 'Q3 2026', DATE '2026-10-15', 'draft'),
('RPT-NBP-014', 'NBP', 'Sprawozdanie płynnościowe LCR/NSFR', 'Wrzesień 2026', DATE '2026-10-08', 'submitted'),
('RPT-BFG-003', 'BFG', 'Raport składki gwarancyjnej', 'Q3 2026', DATE '2026-10-20', 'draft'),
('RPT-FATCA-009', 'FATCA/CRS', 'Raport rachunków raportowanych', '2025', DATE '2026-06-30', 'accepted'),
('RPT-KNF-002', 'KNF', 'Raport dużych ekspozycji', 'Q3 2026', DATE '2026-10-25', 'draft');

--changeset robert_neat:seed-report_submission-001 context:mock-data
INSERT INTO report_submission (regulatory_report_id, gateway_submission_id, gateway_status,
                               validation_errors, submitted_at, resolved_at) VALUES
('RPT-NBP-014', '7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14', 'PENDING', NULL,
 TIMESTAMP WITH TIME ZONE '2026-10-03 11:00:00+02:00', NULL),
('RPT-FATCA-009', 'c19e8d42-0a7b-4f3e-8b65-91d4e2f7a308', 'ACCEPTED', NULL,
 TIMESTAMP WITH TIME ZONE '2026-06-20 10:00:00+02:00', TIMESTAMP WITH TIME ZONE '2026-06-21 09:30:00+02:00');
