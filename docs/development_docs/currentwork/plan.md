## Do zrobienia

Kolejność wg zwrotu z inwestycji (wartość funkcjonalna / nakład). Zakres: rozszerzenia istniejących modułów, bez nowych serwisów i bez refaktoru architektury.

1. **KRD — wniosek po odrzuceniu i domknięcie cyklu** (`banking_backend` `loan/LoanActionsService`, `LoanController`; `banking_web` `loan-detail.page.ts`):
   - przy approvalu `REJECTED` `advance-stage` cofa wniosek do `application` (analogicznie do raportu `REJECTED`→`draft`), żeby można go było złożyć ponownie;
   - dodać `POST /loans/{id}/disburse` (`contract`→`disbursement`→`schedule`), z audytem `LOAN_STAGE_CHANGED`.

   ROI: dziś odrzucony wniosek utyka w `decision` (stałe `409`), a etapy `disbursement`/`schedule` są nieosiągalne mimo `CHECK` i steppera w UI.
2. **KRD-04 — fallback harmonogramu z parametrów wniosku** (`banking_web` `features/loans/loan-detail.page.ts`): `buildSchedule` ma brać `interestRate`/`termMonths` z `LoanDetailDto` zamiast stałych `0.079`/`12`. ROI: kilka linii, a harmonogram 11 wniosków bez wierszy `installment` staje się spójny z danymi backendu.
3. **KKT — wybór klienta i powiązanie dyspozycji ze sprawą** (`banking_web` `features/contact-center/contact-center.store.ts`, `.page.*`; `banking_backend` `contactcenter/`):
   - selektor klienta zasilany `GET /customers` zamiast stałej `CUST-001`;
   - `GET /contact-center/dispositions` i `GET /contact-center/history` filtrowane parametrem `customerId`;
   - `PUT /contact-center/dispositions/{id}/case` (`{caseId}`), żeby powiązać zlecenie ze sprawą (krok F8 bez endpointu).

   ROI: widok 360 staje się realnie używalny dla 12 klientów, a endpoint profilu już przyjmuje `customerId`.
4. **ZJC-03 — rodzaj zajęcia i kwota wolna** (`banking_backend` `enforcement/RegisterEnforcementRequest`, `EnforcementActionsService`; `banking_web` kreator zajęcia): przyjąć `kind` (`ALIMONY|NON_ALIMONY`) zamiast stałej `NON_ALIMONY`, walidować `protectedAmount`/`blockedAmount` względem limitu kwoty wolnej per `kind` (`400`). Limity jako konfigurowalne properties, wartości do potwierdzenia przez użytkownika i dodać ostrzeżenie w kreatorze. ROI: kolumna `kind` już istnieje. To realizacja standardu „Ochrona kwoty wolnej” małą regułą walidacyjną.
5. **CAS — automatyczna eskalacja SLA** (`banking_backend` `cases/`, nowy `@Scheduled` + `@EnableScheduling`, brak dziś w kodzie): cykliczny przegląd spraw z `sla_due_at < now` i `status IN ('new','inProgress')`. Zadanie reużywa logiki `CaseActionsService.escalate` (`escalationSource='AUTO_SLA'`). Filtr statusu to jedyna ochrona przed duplikatami `AUTO_ESCALATED`, bo `governance_mock` nie deduplikuje. Interwał konfigurowalny w properties. ROI: zamyka pozycję planowaną F8 bez zmian w API ani UI.
6. **Integralność danych — nowe changesety Liquibase** (`banking_backend` `db/changelog/schema/`, NIE edytować wdrożonych changesetów):
   - `UNIQUE` na `ebanking_user.username`;
   - `CHECK case_type IN ('complaint','restructuring_request','inquiry')`, ta sama lista walidowana w `CaseActionsService` (`400` zamiast `500` z bazy). Lista jest zgodna z `CASE_TYPES` w `cases.page.ts`;
   - nowy seed `context:mock-data` uzupełniający łańcuch `loan_stage_transition` dla `LN-80203`, z aktualizacją `SeedMockDataEnabledTest.EXPECTED_COUNTS`.

   ROI: zamyka 3 z 9 otwartych punktów w `banking_backend.md` (pkt 16, 17, 19) przy minimalnym nakładzie.
7. **Synchronizacja dokumentacji ze stanem kodu** (`docs/development_docs/services/banking_backend.md` pkt 13 i sekcja F6, `services/external_institutions_gateway_mock.md` „Seedowanie startowe”, `banking_backend/data_model.md` sekcja DORA): poprawić opisy na stan bieżący. Seed `card_processor_mock` zna UUID autoryzacji i chargebacku z `003-card.sql`, seedowane `enforcement_case` mają `gateway_enforcement_id` zgodne z seedem bramy, DSH-05 jest już podłączone do `GET /ict-incidents`, a `block`/`unblock`/`chargebacks` w `card_processor_mock` respektują `simulate-outage` (sekcja F4 w `banking_backend.md`). ROI: usuwa fałszywe „braki”, które kierują kolejne prace w złą stronę.
8. **Lint w modułach bez lintera** (`banking_web` — skrypt `lint` z `angular-eslint`; `banking_backend` — Checkstyle lub Spotless w `pom.xml`): wymaga akceptacji użytkownika co do wyboru narzędzia (zasada 3). ROI: bez tego nie da się wykonać kroku weryfikacji z zasady 2 dla dwóch głównych modułów. Nie daje wartości funkcjonalnej, dlatego jest na końcu listy.

Poza zakresem tej iteracji (nakład znacznie powyżej 20%): trwały storage w serwisach mockowych (`ConcurrentHashMap` → JPA), wspólna klasa bazowa klientów REST (`*Client` + `*UnavailableException`→`503`), filtr `status`/`severity` w `governance_mock` `GET /ict-incidents` (filtrowanie robi już `DashboardService`).
