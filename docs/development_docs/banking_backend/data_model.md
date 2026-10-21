# Model danych banking_backend

Stan docelowy (planowany do implementacji). Źródła: dane mockowe frontendu
`apps/banking_web/src/app/features/**/*.data.ts` + `*.model.ts`, oraz kontrakty
`docs/{card_processor_mock,credit_risk_mock,external_institutions_gateway_mock,governance_mock,identity_access_mock}_contract/api.md`.

## Zasada własności danych

`banking_backend` jest jedynym modułem piszącym do PostgreSQL (zgodnie z komentarzem w
`deploy/compose.yml`). Serwisy mockowe trzymają WŁASNY stan tylko w pamięci (audyt, kolejka
zatwierdzeń, incydenty ICT, OTP, urządzenia, historia zapytań BIK, autoryzacje kart, chargebacki,
zgłoszenia do bramy sprawozdawczej, eksporty FATCA/CRS, przychodzące zajęcia komornicze) —
`banking_backend` NIE duplikuje tych tabel. Przechowuje tylko referencyjne identyfikatory
zwrócone przez mocki (`bureau_inquiry_id`, `approval_id`, `audit_event_id`, `gateway_submission_id`,
`gateway_enforcement_id`, `card_authorization_id`, `chargeback_id`, `otp_id`, `ict_incident_id`) —
jako `varchar`, z `UNIQUE` tam, gdzie import musi być idempotentny.

Statusy przechowywane jako `varchar` z `CHECK` — literały identyczne z frontendem (np.
`inProgress`, `pendingReset`, `stage1`), żeby `*_SEVERITY` mapy w UI nie wymagały zmian po
podłączeniu backendu. Kwoty: `numeric(19,2)` → `BigDecimal`. Klucze biznesowe (`LN-80203`,
`EB-1001`, `PRC-00001`, `CS-10219` itd.) jako PK `varchar` — zachowują zgodność z istniejącymi
przykładami w danych mockowych i kontraktach, bez problemu z sekwencjami przy seedowaniu.

## Encje wspólne

### `customer`
Źródło: pole `customer`/`applicant`/`holder` (wolny tekst) powtarzające się w loans, cards, risk,
enforcement, collections, cases, contact-center. Centralizacja pod FK.

| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(20) PK | np. `CUST-001` |
| full_name | varchar(200) | |
| segment | varchar(20) | `retail`\|`business` |
| tax_id | varchar(30) | NIP/PESEL maskowany w prezentacji (RODO) — potrzebny dla FATCA/CRS `taxId` |
| risk_rating | varchar(10) | wolny string (BBB, BB-, A-, CCC, A) — zgodnie z `CollateralEntry.rating` |

### `bank_account`
Potrzebne dla F6 (blokada środków) i FATCA/CRS (`balance`).

| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(20) PK | |
| customer_id | varchar(20) FK → customer | |
| masked_number | varchar(34) | |
| balance | numeric(19,2) | |
| blocked_amount | numeric(19,2) | suma aktywnych zajęć/blokad |

### `employee`
Źródło: `DemoAccount`/`register.page.ts`. BEZ hasła — uwierzytelnianie żyje w `identity_access_mock`.

| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | format `PRC-00000` |
| first_name, last_name | varchar(100) | |
| email, phone | varchar(150)/varchar(30) | |
| department, branch, position | varchar(100) | |
| supervisor_id | varchar(10) FK → employee, nullable | |
| role | varchar(50) | wolny string, 6 wartości demo |
| business_lines | varchar(200) | CSV kluczy (retail/corporate/risk/collections/compliance/it) |
| start_date | date | |
| status | varchar(20) | `PENDING_APPROVAL`\|`ACTIVE` — zgodnie z identity_access_mock |

Używane jako FK dla `owner` sprawy, `requestedBy`/`decidedBy` zatwierdzeń (referencyjnie, approval
queue żyje w governance_mock).

## Kredyty i pożyczki (KRD, F2, F3)

### `loan_application`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | `LN-80xxx` |
| customer_id | varchar(20) FK → customer | (`applicant` w danych mock) |
| segment | varchar(10) | `retail`\|`business` |
| product | varchar(20) | `cash`\|`mortgage`\|`overdraft`\|`investment`\|`creditLine` |
| stage | varchar(20) | `application`\|`scoring`\|`affordability`\|`decision`\|`contract`\|`disbursement`\|`schedule`\|`collections` |
| requested_amount | numeric(19,2) | |
| approved_amount | numeric(19,2), nullable | |
| score_result | int, nullable | z `credit_risk_mock` `/credit-bureau/score` |
| dsti | numeric(4,3), nullable | z `credit_risk_mock` |
| bureau_inquiry_id | varchar(36), nullable | referencja do historii BIK w credit_risk_mock |
| interest_rate | numeric(5,4) | obecnie hardcoded 0.079 we frontendzie — pole per wniosek, żeby harmonogram mógł być odtwarzalny |
| term_months | int | obecnie wyliczane z liczby rat generowanych lokalnie — trzeba przechowywać |
| created_at | timestamptz | |

### `loan_stage_transition`
Historia przejść cyklu życia (EBA loan origination/monitoring) + link do zatwierdzenia maker-checker.

| kolumna | typ | uwagi |
|---|---|---|
| id | bigint PK identity | |
| loan_application_id | varchar(10) FK → loan_application | |
| from_stage, to_stage | varchar(20) | |
| approval_id | varchar(36), nullable | referencja do governance_mock (`POST /approvals` dla przejścia do `decision`) |
| transitioned_at | timestamptz | |
| transitioned_by | varchar(10) FK → employee | |

### `installment`
F2: harmonogram generowany przez `banking_backend` (obecnie liczony runtime we frontendzie) — trwały, bo raty mają status (`paid`/`upcoming`/`overdue`) niezależny od wzoru.

| kolumna | typ | uwagi |
|---|---|---|
| id | bigint PK identity | |
| loan_application_id | varchar(10) FK → loan_application | |
| installment_no | int | |
| due_date | date | |
| principal, interest | numeric(19,2) | |
| status | varchar(10) | `paid`\|`upcoming`\|`overdue` |

## Karty płatnicze (KRT, F4)

### `payment_card`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | |
| customer_id | varchar(20) FK → customer | (`holder` w danych mock) |
| masked_number | varchar(19) | TYLKO zamaskowany numer (PCI DSS — nigdy pełny PAN) |
| network | varchar(12) | `Visa`\|`Mastercard` |
| product | varchar(50) | |
| status | varchar(10) | `active`\|`blocked`\|`pending`\|`expired` — zsynchronizowane z `card_processor_mock` `/cards/{id}/block-status` |
| expiry | varchar(7) | `YYYY-MM` |
| limit_daily, limit_atm | numeric(19,2) | |

### `card_authorization_ref`
Referencja do autoryzacji żyjącej w `card_processor_mock` (ten serwis jest source of truth) — przechowywana lokalnie tylko, by dashboard/KRT-04 mógł łączyć FK bez odpytywania mocka za każdym razem.

| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(36) PK | = `card_processor_mock` authorization id |
| card_id | varchar(10) FK → payment_card | |
| merchant | varchar(200) | |
| amount | numeric(19,2) | |
| occurred_at | timestamptz | |
| processor | varchar(12) | `Visa`\|`Mastercard` |
| approved | boolean | |

### `chargeback_ref`
Referencja analogiczna, source of truth w `card_processor_mock`.

| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(36) PK | = `card_processor_mock` chargeback id |
| card_authorization_ref_id | varchar(36) FK → card_authorization_ref | |
| reason_code | varchar(4) | `4855`\|`4837`\|`4853`\|`4863` |
| status | varchar(20) | `FILED`\|`UNDER_REVIEW`\|`RESOLVED_CUSTOMER`\|`RESOLVED_MERCHANT` |
| filed_at | timestamptz | |

## Ryzyko kredytowe (RSK, F3)

### `collateral_entry`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | |
| customer_id | varchar(20) FK → customer | |
| type | varchar(20) | `mortgage`\|`pledge`\|`guarantee` |
| valuation | numeric(19,2) | |
| ltv | numeric(5,2) | |
| rating | varchar(10) | wolny string (BBB, BB-, A-, CCC, A) |
| stage | varchar(10) | `stage1`\|`stage2`\|`stage3` — zasilane z `credit_risk_mock` `/ecl/classify` |
| provision | numeric(19,2) | |
| ecl_classified_at | timestamptz, nullable | kiedy ostatnio przeliczone przez credit_risk_mock |

## Bankowość elektroniczna (EBK, F5)

### `ebanking_user`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | `EB-1001` |
| customer_id | varchar(20) FK → customer | |
| username | varchar(100) UNIQUE | login do e-bankingu |
| channel | varchar(10) | `web`\|`mobile` |
| access_status | varchar(15) | `active`\|`locked`\|`pendingReset` — przełączane po `identity_access_mock` `/ebanking-access/{id}/unlock` |
| daily_limit | numeric(19,2) | |
| last_login | timestamptz | |
| permissions | varchar(500) | CSV wolnych stringów PL |

Urządzenia mobilne (EBK-03 device binding) żyją w `identity_access_mock` (`/devices/{ownerId}`) —
BEZ duplikatu tabeli po stronie `banking_backend`; `ownerId` dla klienta e-bankingu to `ebanking_user.id`.

## Kanał kontaktu (KKT)

### `service_disposition`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | |
| customer_case_id | varchar(10) FK → customer_case | (`linkedCase`) |
| type_key | varchar(50) | |
| status | varchar(15) | współdzielony enum `CaseStatus` |
| occurred_at | date | |

### `contact_history_entry`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | |
| customer_id | varchar(20) FK → customer | |
| channel_key | varchar(30) | |
| occurred_at | date | |
| summary | varchar(500) | |

`ChatMessage` i Klient 360 (`CustomerProfile`) NIE są tabelami — czat jest czysto frontendowy (nic do
seedowania), a widok 360 jest zapytaniem agregującym `customer` + liczniki `loan_application`,
`payment_card`, `customer_case` (nie przechowujemy zdenormalizowanych liczników).

## Sprawy klientów (CAS, F8)

### `customer_case`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | `CS-10219` |
| customer_id | varchar(20) FK → customer | |
| subject | varchar(300) | |
| owner_id | varchar(10) FK → employee | |
| priority | varchar(10) | `low`\|`medium`\|`high` |
| case_type | varchar(30) CHECK IN ('complaint','restructuring_request','inquiry') | steruje terminem ustawowym 30/60 dni (ustawa o reklamacjach); lista zgodna z `CASE_TYPES` w `cases.page.ts` i walidowana też w `CaseActionsService` (400 przed insertem) |
| registered_at | timestamptz | |
| statutory_deadline | date | wyliczany z `case_type` + `registered_at`, nie trzymany jako wolny tekst |
| sla_due_at | timestamptz | zamiast `slaRemainingHours` (countdown) — pochodna, licz różnicę do `sla_due_at` w zapytaniu |
| status | varchar(15) | `new`\|`inProgress`\|`escalated`\|`resolved` |
| escalated_at | timestamptz, nullable | |
| escalation_source | varchar(10), nullable | `MANUAL`\|`AUTO_SLA` — `AUTO_SLA` koreluje z `governance_mock` `AUTO_ESCALATED`, zapisywane RAZ (deduplikacja, bo governance_mock jej nie robi) |
| governance_audit_event_id | varchar(36), nullable | ostatni powiązany wpis audytowy z governance_mock |

`RecentCase`/`RegulatoryAlert` na dashboardzie to zapytania (TOP N po `registered_at`/`due_date`),
NIE tabele — obecne duplikaty w `dashboard.data.ts` są artefaktem mocka frontendowego.

## Windykacja (WND, F3)

### `collections_case`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | |
| loan_application_id | varchar(10) FK → loan_application | |
| days_past_due | int | |
| outstanding_amount | numeric(19,2) | |
| stage | varchar(20) | `monitoring`\|`reminder`\|`settlement`\|`restructuring`\|`external`\|`legal` |

### `collections_action`
Historia akcji WND-02/03/04 (wezwanie, ugoda, przekazanie zewnętrzne) — obecnie frontend tylko robi
toast, backend musi trzymać historię.

| kolumna | typ | uwagi |
|---|---|---|
| id | bigint PK identity | |
| collections_case_id | varchar(10) FK → collections_case | |
| action_type | varchar(20) | `REMINDER_SENT`\|`SETTLEMENT_PROPOSED`\|`EXTERNAL_TRANSFER` |
| performed_at | timestamptz | |
| performed_by | varchar(10) FK → employee | |
| governance_audit_event_id | varchar(36), nullable | |

## Zajęcia komornicze (ZJC, F6)

### `enforcement_case`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(10) PK | lokalny ID `banking_backend` |
| gateway_enforcement_id | varchar(36), UNIQUE, nullable | ID w `external_institutions_gateway_mock` — klucz idempotencji przy pollingu `GET /enforcement` |
| customer_id | varchar(20) FK → customer | |
| bank_account_id | varchar(20) FK → bank_account, nullable | środki blokowane na koncie |
| bailiff | varchar(200) | |
| case_number | varchar(50) | |
| kind | varchar(15) | `ALIMONY`\|`NON_ALIMONY` — steruje limitem kwoty wolnej od zajęcia |
| blocked_amount, transferred_amount, protected_amount | numeric(19,2) | `transferred_amount` nullable do rozliczenia |
| status | varchar(10) | `registered`\|`active`\|`settled`\|`closed` |
| approval_id | varchar(36), nullable | referencja do governance_mock dla przekazania (maker-checker) |

## Raportowanie regulacyjne (RPT, F7)

### `regulatory_report`
| kolumna | typ | uwagi |
|---|---|---|
| id | varchar(15) PK | `RPT-FATCA-009` (13 znaków) |
| recipient | varchar(10) | `KNF`\|`NBP`\|`BFG`\|`FATCA/CRS` |
| name | varchar(200) | |
| period | varchar(20) | wolny tekst, np. `Q3 2026`, `Wrzesień 2026` |
| due_date | date | |
| status | varchar(15) | `draft`\|`submitted`\|`accepted` |

### `report_submission`
Historia wywołań `external_institutions_gateway_mock` `/regulatory-gateway/submit` — status bramy
(`PENDING|ACCEPTED|REJECTED`) jest osobnym kontraktem od `regulatory_report.status`.

| kolumna | typ | uwagi |
|---|---|---|
| id | bigint PK identity | |
| regulatory_report_id | varchar(15) FK → regulatory_report | |
| gateway_submission_id | varchar(36) UNIQUE | |
| gateway_status | varchar(15) | `PENDING`\|`ACCEPTED`\|`REJECTED` |
| validation_errors | varchar(1000), nullable | CSV/JSON tekstowy komunikatów |
| submitted_at | timestamptz | |
| resolved_at | timestamptz, nullable | |

FATCA/CRS XML (`external_institutions_gateway_mock` `/fatca-crs/export`) NIE jest przechowywany w
`banking_backend` — mock jest źródłem prawdy dla wygenerowanego pliku; `banking_backend` trzyma tylko
że eksport nastąpił, poprzez `report_submission` z `recipient='FATCA/CRS'`.

## Pulpit (DSH)

### `dashboard_kpi_snapshot`
LCR/NSFR i inne KPI nie są wyliczalne z innych tabel w tej iteracji (brak modelu płynności/kapitału)
— migawka okresowa, najnowsza wykorzystywana do prezentacji.

| kolumna | typ | uwagi |
|---|---|---|
| id | bigint PK identity | |
| snapshot_date | date | |
| active_loans_count | int | |
| loan_portfolio_total | numeric(19,2) | |
| cards_issued_count | int | |
| npl_ratio | numeric(5,2) | |
| lcr_ratio | numeric(5,2) | próg regulacyjny 100% (CRR/Basel III) |
| nsfr_ratio | numeric(5,2) | |
| open_cases_count | int | |
| collections_overdue_total | numeric(19,2) | |

### `portfolio_trend_point`
| kolumna | typ | uwagi |
|---|---|---|
| id | bigint PK identity | |
| snapshot_date | date | |
| portfolio_total | numeric(19,2) | |

Wykres "portfel według produktu" (DSH-03) to zapytanie agregujące `loan_application` po `product` —
nie tabela.

## Zgodność i incydenty (DORA, cross-cutting)

Rejestr incydentów ICT (`POST /ict-incidents`) żyje wyłącznie w `governance_mock` — `banking_backend`
NIE ma własnej tabeli; `DSH-05` odpytuje `governance_mock` `GET /ict-incidents` na żywo (poza zakresem
tej sesji — REST integracja to kolejna iteracja).

## Zmienna środowiskowa seedowania

`BANKING_SEED_MOCK_DATA` (bool, `true`/`false`) — gdy `true`, Liquibase aktywuje context `mock-data`
i wczytuje changesety z danymi 1:1 z `*.data.ts` (oraz `DEMO_ACCOUNTS` z `login.page.ts`, bez hasła —
hasła logowania żyją tylko w `identity_access_mock`). Gdy `false`/nieustawiona — tylko schemat, tabele
puste. Domyślna wartość `true` w `.env.example` (dev lokalny), bez wartości w `deploy/compose.yml`
produkcyjnym (wymaga jawnego ustawienia w `config.env` na hoście, jeśli potrzebne).
