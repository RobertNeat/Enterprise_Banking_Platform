--liquibase formatted sql

--changeset robert_neat:seed-regulatory_report-001 context:mock-data
-- due_date is relative to CURRENT_DATE at seed time (old fixed anchor was 2026-10-05). 'period'
-- labels keep their original calendar text (illustrative only, not read by any backend logic).
-- RPT-KNF-002's due_date is deliberately moved into the past (now - 5d, status stays 'draft') —
-- it is the only non-'accepted' report that can be overdue (RPT-FATCA-009 is 'accepted' and
-- DashboardService.findRegulatoryAlerts excludes 'accepted' reports), so the dashboard's
-- regulatory-alerts list always has at least one genuinely overdue entry, not just near-term ones.
INSERT INTO regulatory_report (id, recipient, name, period, due_date, status) VALUES
('RPT-KNF-001', 'KNF', 'Sprawozdanie adekwatności kapitałowej', 'Q3 2026', CAST(CURRENT_DATE + 10 AS DATE), 'draft'),
('RPT-NBP-014', 'NBP', 'Sprawozdanie płynnościowe LCR/NSFR', 'Wrzesień 2026', CAST(CURRENT_DATE + 3 AS DATE), 'submitted'),
('RPT-BFG-003', 'BFG', 'Raport składki gwarancyjnej', 'Q3 2026', CAST(CURRENT_DATE + 15 AS DATE), 'draft'),
('RPT-FATCA-009', 'FATCA/CRS', 'Raport rachunków raportowanych', '2025', CAST(CURRENT_DATE - 97 AS DATE), 'accepted'),
('RPT-KNF-002', 'KNF', 'Raport dużych ekspozycji', 'Q3 2026', CAST(CURRENT_DATE - 5 AS DATE), 'draft');

--changeset robert_neat:seed-report_submission-001 context:mock-data
-- submitted_at/resolved_at keep the same offsets relative to their report's due_date as the old
-- fixed-anchor seed (RPT-NBP-014: submitted 5 days before due_date; RPT-FATCA-009: submitted/resolved
-- ~10/9 days before its due_date), so the two stay internally consistent after the shift.
INSERT INTO report_submission (regulatory_report_id, gateway_submission_id, gateway_status,
                               validation_errors, submitted_at, resolved_at) VALUES
('RPT-NBP-014', '7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14', 'PENDING', NULL,
 CURRENT_TIMESTAMP - INTERVAL '2' DAY, NULL),
('RPT-FATCA-009', 'c19e8d42-0a7b-4f3e-8b65-91d4e2f7a308', 'ACCEPTED', NULL,
 CURRENT_TIMESTAMP - INTERVAL '107' DAY, CURRENT_TIMESTAMP - INTERVAL '106' DAY);
