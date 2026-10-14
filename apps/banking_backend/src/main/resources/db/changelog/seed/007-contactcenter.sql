--liquibase formatted sql

--changeset robert_neat:seed-service_disposition-001 context:mock-data
-- DSP-771 (cards.limitDaily) matches the chat/contact history of CUST-001 (Jan Wisniewski,
-- CUSTOMER_PROFILE in contact-center.data.ts) on 2026-10-05 - relinked from CS-10231 (different
-- customer) to CS-10229, CUST-001's only case in the seed (no dedicated card-limit case yet).
-- DSP-765 (ebanking.unlock, resolved 2026-10-02) actually matches CUST-002 (Katarzyna Zajac):
-- CS-10219 is CUST-002's case (resolved, deadline 2026-10-02) and EB-1002 (CUST-002) is active -
-- kept linked to CS-10219, which is topically a card complaint, not e-banking, but it is CUST-002's
-- only case in the seed; a precise fix needs a dedicated e-banking case for CUST-002.
INSERT INTO service_disposition (id, customer_case_id, type_key, status, occurred_at) VALUES
('DSP-771', 'CS-10229', 'cards.limitDaily', 'inProgress', DATE '2026-10-05'),
('DSP-765', 'CS-10219', 'ebanking.unlock', 'resolved', DATE '2026-10-02'),
('DSP-758', 'CS-10225', 'loans.decisionMove', 'new', DATE '2026-09-29');

--changeset robert_neat:seed-contact_history_entry-001 context:mock-data
-- All entries belong to CUSTOMER_PROFILE (Jan Wiśniewski); occurred_at is a DATE column.
INSERT INTO contact_history_entry (id, customer_id, channel_key, occurred_at, summary) VALUES
('CH-4401', 'CUST-001', 'contact.channel.chat', DATE '2026-10-05', 'Prośba o podniesienie limitu karty'),
('CH-4390', 'CUST-001', 'contact.channel.phone', DATE '2026-09-21', 'Pytanie o harmonogram spłat kredytu'),
('CH-4375', 'CUST-001', 'contact.channel.branch', DATE '2026-08-30', 'Wizyta w oddziale — aktualizacja danych');
