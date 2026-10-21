--liquibase formatted sql

--changeset robert_neat:seed-regulatory_submission-001 context:mock-data
-- Fixed gateway-side submission ids matching apps/banking_backend's report_submission seed
-- (db/changelog/seed/010-reporting.sql) so "Sprawdź status" resolves against seeded reports, not
-- just newly submitted ones. Previously seeded in RegulatoryGatewayService's @PostConstruct
-- seed() method. RPT-NBP-014 is seeded PENDING -- RegulatoryGatewayService.reschedulePendingSubmissions()
-- (an ApplicationReadyEvent listener) picks it up at startup and schedules its resolve() the same
-- way submit() does, so it does not stay PENDING forever.
INSERT INTO gateway.regulatory_submission
    (submission_id, report_id, recipient, name, period, status, submitted_at, resolved_at)
VALUES
    ('7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14', 'RPT-NBP-014', 'NBP', 'Sprawozdanie płynnościowe LCR/NSFR',
     'Wrzesień 2026', 'PENDING', '2026-10-03T09:00:00Z', NULL),
    ('c19e8d42-0a7b-4f3e-8b65-91d4e2f7a308', 'RPT-FATCA-009', 'FATCA/CRS', 'Raport rachunków raportowanych',
     '2025', 'ACCEPTED', '2026-06-20T08:00:00Z', '2026-06-21T07:30:00Z');
