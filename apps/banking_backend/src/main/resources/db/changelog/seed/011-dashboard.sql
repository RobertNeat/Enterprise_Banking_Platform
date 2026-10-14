--liquibase formatted sql

--changeset robert_neat:seed-dashboard_kpi_snapshot-001 context:mock-data
-- KPI values from dashboard.page.html (512,4 mln PLN portfolio, 24,6 mln PLN overdue).
INSERT INTO dashboard_kpi_snapshot (snapshot_date, active_loans_count, loan_portfolio_total,
                                    cards_issued_count, npl_ratio, lcr_ratio, nsfr_ratio,
                                    open_cases_count, collections_overdue_total) VALUES
(DATE '2026-10-05', 18420, 512400000.00, 64910, 3.80, 142.00, 118.00, 312, 24600000.00);

--changeset robert_neat:seed-portfolio_trend_point-001 context:mock-data
-- dashboard.page.ts trend [412..512] mln PLN, Jan-Sep 2026.
INSERT INTO portfolio_trend_point (snapshot_date, portfolio_total) VALUES
(DATE '2026-01-05', 412000000.00),
(DATE '2026-02-05', 428000000.00),
(DATE '2026-03-05', 441000000.00),
(DATE '2026-04-05', 455000000.00),
(DATE '2026-05-05', 468000000.00),
(DATE '2026-06-05', 479000000.00),
(DATE '2026-07-05', 491000000.00),
(DATE '2026-08-05', 503000000.00),
(DATE '2026-09-05', 512000000.00);
