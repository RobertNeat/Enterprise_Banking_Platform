## Do zrobienia

Kolejność wg zwrotu z inwestycji (wartość funkcjonalna / nakład). Zakres: rozszerzenia istniejących modułów, bez nowych serwisów i bez refaktoru architektury.

1. **CAS — automatyczna eskalacja SLA** (`banking_backend` `cases/`, nowy `@Scheduled` + `@EnableScheduling`, brak dziś w kodzie): cykliczny przegląd spraw z `sla_due_at < now` i `status IN ('new','inProgress')`. Zadanie reużywa logiki `CaseActionsService.escalate` (`escalationSource='AUTO_SLA'`). Filtr statusu to jedyna ochrona przed duplikatami `AUTO_ESCALATED`, bo `governance_mock` nie deduplikuje. Interwał konfigurowalny w properties. ROI: zamyka pozycję planowaną F8 bez zmian w API ani UI.
2. **Integralność danych — nowe changesety Liquibase** (`banking_backend` `db/changelog/schema/`, NIE edytować wdrożonych changesetów):
   - `UNIQUE` na `ebanking_user.username`;
   - `CHECK case_type IN ('complaint','restructuring_request','inquiry')`, ta sama lista walidowana w `CaseActionsService` (`400` zamiast `500` z bazy). Lista jest zgodna z `CASE_TYPES` w `cases.page.ts`;
   - nowy seed `context:mock-data` uzupełniający łańcuch `loan_stage_transition` dla `LN-80203`, z aktualizacją `SeedMockDataEnabledTest.EXPECTED_COUNTS`.

   ROI: zamyka 3 z 9 otwartych punktów w `banking_backend.md` (pkt 16, 17, 19) przy minimalnym nakładzie.
3. **Synchronizacja dokumentacji ze stanem kodu** (`docs/development_docs/services/banking_backend.md` pkt 13 i sekcja F6, `services/external_institutions_gateway_mock.md` „Seedowanie startowe”, `banking_backend/data_model.md` sekcja DORA): poprawić opisy na stan bieżący. Seed `card_processor_mock` zna UUID autoryzacji i chargebacku z `003-card.sql`, seedowane `enforcement_case` mają `gateway_enforcement_id` zgodne z seedem bramy, DSH-05 jest już podłączone do `GET /ict-incidents`, a `block`/`unblock`/`chargebacks` w `card_processor_mock` respektują `simulate-outage` (sekcja F4 w `banking_backend.md`). ROI: usuwa fałszywe „braki”, które kierują kolejne prace w złą stronę.
4. **Lint w modułach bez lintera** (`banking_web` — skrypt `lint` z `angular-eslint`; `banking_backend` — Checkstyle lub Spotless w `pom.xml`): wymaga akceptacji użytkownika co do wyboru narzędzia (zasada 3). ROI: bez tego nie da się wykonać kroku weryfikacji z zasady 2 dla dwóch głównych modułów. Nie daje wartości funkcjonalnej, dlatego jest na końcu listy.

Poza zakresem tej iteracji (nakład znacznie powyżej 20%): trwały storage w serwisach mockowych (`ConcurrentHashMap` → JPA), wspólna klasa bazowa klientów REST (`*Client` + `*UnavailableException`→`503`), filtr `status`/`severity` w `governance_mock` `GET /ict-incidents` (filtrowanie robi już `DashboardService`).
