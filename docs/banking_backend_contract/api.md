# banking_backend — kontrakt API

Serwis eksponowany na hoście: `http://localhost:10520`. Wewnątrz sieci compose:
`http://banking_backend:10520` (nazwa hosta zgodna z nazwą usługi w `deploy/compose.yml`). Wołany
bezpośrednio z przeglądarki przez `banking_web` (CORS skonfigurowany w security filter chain, origin
konfigurowalny przez property `app.cors.allowed-origin` / zmienną środowiskową `BANKING_WEB_ORIGIN`,
domyślnie `http://localhost:10500`; metody `GET, POST, PUT, PATCH, DELETE, OPTIONS`, nagłówki
`Authorization, Content-Type`).

Wszystkie endpointy GET-only poza zakresem auth (21 encji) — bez wiązania akcji/mutacji (świadomie
poza zakresem tej iteracji). Serializacja JSON przez Jackson (transytywnie z
`spring-boot-starter-web`, bez nowej zależności). DTO to Java records; encje JPA nigdy nie są
serializowane bezpośrednio.

Odpowiedzi błędów (404 i inne):

```json
{ "message": "opis błędu" }
```

## Auth (JWT)

`banking_backend` jest jedynym pośrednikiem auth dla `banking_web` — `identity_access_mock` NIE jest
wołany bezpośrednio z przeglądarki (mimo że eksponuje port na hoście dla narzędzi operatorskich).

### POST /auth/login

Publiczny (`permitAll`). Request: `{ "employeeId": "PRC-00001", "password": "Admin12345" }` —
przekazywany 1:1 do `identity_access_mock` `/auth/login`. Na podstawie odpowiedzi (`role`,
`permissions`) wydaje własny JWT podpisany HS256 (sekret `banking.jwt.secret` / `BANKING_JWT_SECRET`,
min. 32 bajty, bez defaultu w kodzie — wymagany).

Response `200`:

```json
{ "token": "<jwt>", "employeeId": "PRC-00001", "role": "Administrator systemu", "permissions": ["dashboard", "..."], "seniorityLevel": 3 }
```

`role` w body to tekst PL (do wyświetlenia), permissions 1:1 z `identity_access_mock` (bez
duplikowania tabeli rola→permission w backendzie). `seniorityLevel` (dodane R1.5) — hierarchia ról
NIE istniejąca w `identity_access_mock`, zdefiniowana tylko tutaj (`auth/RoleCode.java`):
`admin`=3, `compliance`=2, `loans`/`risk`/`collections`/`contact`=1. Używana przez kolejkę
zatwierdzeń (maker-checker, patrz "Approvals" niżej). Claimy JWT: `sub`=employeeId, `role`=stabilny
kod (`admin`/`loans`/`risk`/`collections`/`contact`/`compliance` — mapowanie w `auth/RoleCode.java`),
`permissions` (lista), `seniorityLevel` (int), `iat`, `exp` (+8h).

Błędy: `401`/`403`/`400` przekazane 1:1 z `identity_access_mock`; `502` jeśli `identity_access_mock`
zwróci rolę nierozpoznaną w `RoleCode`; `503` jeśli `identity_access_mock` jest nieosiągalny.

### POST /auth/logout

Wymaga `Authorization: Bearer <jwt>` (JWT wydany przez `banking_backend`, nie token
`identity_access_mock`). Proxy do `identity_access_mock` `/auth/logout` (forward Bearer — no-op tam,
token nieznany temu serwisowi). Response: `204 No Content`.

### POST /auth/register

Publiczny (`permitAll`). Passthrough request/response 1:1 do `identity_access_mock` `/auth/register`
(zob. `docs/identity_access_mock_contract/api.md` dla kształtu). Response: `201` lub błąd (`400`/`409`)
przekazany 1:1.

### Autoryzacja innych endpointów

Wszystkie endpointy poza `/actuator/health`, `POST /auth/login`, `POST /auth/register` wymagają
nagłówka `Authorization: Bearer <jwt>` z tokenem wydanym przez `POST /auth/login` powyżej (nie token
`identity_access_mock`). Brak/niepoprawny/wygasły token → `401`.

Gdy `BANKING_SEED_MOCK_DATA=false` (schemat bez danych): endpointy listujące zwracają `200 OK` z
pustą listą `[]`; endpointy GET-by-id i "najnowszy snapshot" zwracają `404 Not Found`.

## Approvals (proxy do governance_mock, R1.5)

Proxy do `governance_mock`'s `/approvals` (kolejka zatwierdzeń maker-checker) — zob.
`docs/governance_mock_contract/api.md` dla kształtu bazowego. Wymaga JWT jak każdy inny endpoint
poza auth.

### GET /approvals

Query opcjonalny `status` (`PENDING|APPROVED|REJECTED`). Każdy wpis wzbogacony o
`requestedByRole`/`requestedBySeniorityLevel` (rozwiązane z `Employee.role` w banking_backend, NIE z
`identity_access_mock`) — `null`/`null` jeśli rola wnioskodawcy nie mapuje się na `RoleCode`.

```json
{
  "id": "uuid", "type": "LOAN_STAGE_TRANSITION", "requestedBy": "PRC-10234",
  "payload": "...", "status": "PENDING", "createdAt": "...", "decidedBy": null, "decidedAt": null,
  "requestedByRole": "Doradca kredytowy", "requestedBySeniorityLevel": 1
}
```

### GET /approvals/{id}

Jak wyżej, pojedynczy wpis. `404` jeśli nieznany (przekazane z `governance_mock`).

### POST /approvals/{id}/decide

Request: `{ "decision": "APPROVED" }` (`APPROVED|REJECTED`) — **bez** `decidedBy`: backend ustawia
go z `sub` JWT wołającego, nigdy z ciała żądania.

Reguła maker-checker wymuszana PRZED wywołaniem `governance_mock` (więc `governance_mock`'s własny
409 self-approval check praktycznie nie jest już osiągalny stąd — tylko przez bezpośrednie
wywołanie `governance_mock`, poza zakresem tego proxy):

- `403` jeśli wołający jest tą samą osobą co `requestedBy` approvala.
- `403` jeśli rola wnioskodawcy nie mapuje się na `RoleCode` (fail closed).
- `403` jeśli `checker.seniorityLevel <= requesterRole.seniorityLevel()`.
- `404`/`409` przekazane 1:1 z `governance_mock` (nieznany approval / już zdecydowany).
- `200 OK` sukces, zwraca zaktualizowany wpis (jak `GET /approvals/{id}`).

## Macierz pokrycia pól (widok → model TS → backend)

Reguły spłaszczania wspólne dla całego kontraktu:

- `Customer.full_name` → pole `applicant`/`customer`/`holder` we frontendzie (zależnie od widoku).
- `Employee` → inicjał imienia + spacja + nazwisko (np. `PRC-60001` Anna Kowalska → `"A. Kowalska"`),
  zgodnie z konwencją `owner`/`transitionedBy` w danych mockowych frontendu.
- CSV w kolumnie (`employee.business_lines`, `ebanking_user.permissions`) → `string[]` w DTO (split
  po `,`, trim).
- `Instant`/`LocalDate` serializowane jako ISO-8601 przez Jackson. Konwersja `Instant` → data/czas
  lokalny używa strefy `Europe/Warsaw` (`common/BankTimeZone`), NIE UTC — seed zapisuje znaczniki
  czasu jako `+01:00`/`+02:00`; konwersja przez UTC przesuwałaby widoczną godzinę (np.
  `EbankingUser.lastLogin`) nawet gdy data pozostaje ta sama. `EbankingUser.lastLogin` jest
  formatowane w DTO na wzorzec `yyyy-MM-dd HH:mm` (identyczny z `ebanking.data.ts`).
  `ContactHistoryEntry.occurred_at` jest kolumną `DATE` (bez godziny) — zwracane jako `yyyy-MM-dd`,
  patrz GAP w tabeli `contact-center` niżej.

### loans (`loans.page.ts`, `loan-detail.page.ts`)

| pole TS (`LoanApplication`) | backend | uwaga |
|---|---|---|
| `id` | `loan_application.id` | |
| `applicant` | `customer.full_name` (JOIN `customer_id`) | spłaszczone |
| `segment` | `loan_application.segment` | |
| `product` | `loan_application.product` | |
| `stage` | `loan_application.stage` | |
| `requestedAmount` | `loan_application.requested_amount` | |
| `approvedAmount` | `loan_application.approved_amount` (nullable) | |
| `scoreResult` | `loan_application.score_result` (nullable → `0` gdy NULL, zgodnie z `LN-80288` we froncie) | |
| `dsti` | `loan_application.dsti` (nullable → `0` gdy NULL) | |
| `createdAt` | `loan_application.created_at` → `yyyy-MM-dd` | |

`loan-detail.page.ts` zagnieżdża harmonogram (`Installment[]`, pola `no/dueDate/principal/interest/status`)
i etapy (`LOAN_STAGES`, stepper UI). Endpoint szczegółu zwraca jedną odpowiedź złożoną: wniosek +
`installments` (z `installment` tabeli, `installmentNo`→`no`) + `stageTransitions` (z
`loan_stage_transition`, dodatkowe pole nieużywane dziś przez UI, ale wystawione bo "zasilamy
wszystkim do czego backend ma dostęp" — patrz sekcja DTO niżej).

**GAP (udokumentowany, bez zmiany schematu):** tylko 2 z 13 wniosków (`LN-80231`, `LN-80203`) mają
realne wiersze w `installment`. Pozostałe 11 (w tym 5 zaślepek z `collections.data.ts`) nie mają
harmonogramu w seedzie — `data_model.md` opisuje to jako świadomą decyzję z poprzedniej sesji
(„harmonogram generowany przez banking_backend, obecnie liczony runtime we frontendzie"). Backend
wystawia **tylko to, co faktycznie przechowuje**: `installments: []` dla wniosków bez wierszy w
`installment`. Generowanie harmonogramu runtime (`buildSchedule`) pozostaje po stronie frontendu
jako fallback — frontend używa `installments` z API gdy niepuste, inaczej lokalnie generuje tak jak
dotychczas (brak zmiany zachowania UI, brak nowej tabeli). Odnotowane w `docs/services/banking_backend.md`.

`interestRate`/`termMonths` istnieją w backendzie (nie są w `LoanApplication` TS) — wystawione w DTO
szczegółu, nieużywane przez dzisiejszy widok (hardcoded `0.079`/12 we `buildSchedule`), zostawione
do ewentualnego wykorzystania przez frontend w przyszłości.

### cards (`cards.page.ts`)

| pole TS (`PaymentCard`) | backend |
|---|---|
| `id` | `payment_card.id` |
| `holder` | `customer.full_name` |
| `maskedNumber` | `payment_card.masked_number` |
| `network` | `payment_card.network` |
| `product` | `payment_card.product` |
| `status` | `payment_card.status` |
| `expiry` | `payment_card.expiry` |
| `limitDaily` | `payment_card.limit_daily` |
| `limitAtm` | `payment_card.limit_atm` |

| pole TS (`CardAuthorization`) | backend |
|---|---|
| `id` | `card_authorization_ref.id` (UUID) |
| `cardId` | `card_authorization_ref.card_id` |
| `merchant` | `card_authorization_ref.merchant` |
| `amount` | `card_authorization_ref.amount` |
| `date` | `card_authorization_ref.occurred_at` → `yyyy-MM-dd` |
| `processor` | `card_authorization_ref.processor` |
| `approved` | `card_authorization_ref.approved` |

`chargeback_ref` nie ma konsumenta w `cards.page.ts` (brak widoku chargebacków) — wystawiony pod
`/cards/chargebacks` osobno (żądanie „zasilamy wszystkimi danymi, do których backend ma dostęp").

### cases (`cases.page.ts`)

| pole TS (`CustomerCase`) | backend |
|---|---|
| `id` | `customer_case.id` |
| `subject` | `customer_case.subject` |
| `customer` | `customer.full_name` |
| `owner` | `employee` → `"{inicjał}. {nazwisko}"` |
| `priority` | `customer_case.priority` |
| `statutoryDeadline` | `customer_case.statutory_deadline` |
| `slaRemainingHours` | pochodna: `(sla_due_at - now) w godzinach, floor, min 0` |
| `status` | `customer_case.status` |

`slaRemainingHours` jest countdown w `data_model.md` ("pochodna, licz różnicę do `sla_due_at` w
zapytaniu") — liczone w serwisie względem `Instant.now()`, nie przechowywane. Konsekwencja: seed ma
`sla_due_at` zakotwiczone wokół 2026-10-05/09, więc po tej dacie (np. dziś) wszystkie wartości
wychodzą `0` (`Math.max(0, ...)`) — to jest poprawne zachowanie "ile zostało do terminu", nie błąd;
demo z realnym zegarem po prostu przegapiło zakotwiczone terminy seeda.

### collections (`collections.page.ts`)

| pole TS (`CollectionsCase`) | backend |
|---|---|
| `id` | `collections_case.id` |
| `customer` | `customer.full_name` (JOIN przez `loan_application.customer_id`) |
| `loanId` | `collections_case.loan_application_id` |
| `daysPastDue` | `collections_case.days_past_due` |
| `outstandingAmount` | `collections_case.outstanding_amount` |
| `stage` | `collections_case.stage` |

`collections_action` (historia wezwań/ugód) bez konsumenta na liście — wystawiona pod
`/collections/{id}/actions`.

### contact-center (`contact-center.page.ts`)

| pole TS | backend | uwaga |
|---|---|---|
| `ServiceDisposition.id/typeKey/status/date` | `service_disposition` (`type_key`, `status`, `occurred_at`) | |
| `ServiceDisposition.linkedCase` | `service_disposition.customer_case_id` | |
| `ContactHistoryEntry.id/channelKey/summary` | `contact_history_entry` (`channel_key`, `summary`) | |
| `ContactHistoryEntry.date` | `contact_history_entry.occurred_at` | kolumna jest `DATE` (bez godziny) — frontend ma `'2026-10-05 09:12'`; backend zwraca `yyyy-MM-dd` (ucięta godzina, zgodne z typem kolumny, odnotowane jako znana rozbieżność w `data_model.md` pkt 18) |
| `CustomerProfile` | agregat: `customer` + COUNT `loan_application`/`payment_card`/`customer_case` | `riskRating` = `customer.risk_rating`; klient wybrany po `customerId` (parametr), domyślnie `CUST-001` dla zgodności z dzisiejszym statycznym widokiem. Kosmetyczny GAP: `profile.segment` zwraca `customer.segment` (`retail`/`business`), podczas gdy `contact-center.data.ts` miało wolny tekst `'Retail — Premium'` — backend nie przechowuje tej etykiety marketingowej, uznane za drobne/nieistotne dla testów |
| `ChatMessage`/`INITIAL_CHAT` | brak tabeli | poza zakresem — czysto frontendowe (potwierdzone w `data_model.md`) |

`GET /contact-center/profile/{customerId}` — parametr opcjonalny na poziomie frontendu (dziś
zawsze `CUST-001`, przyszła iteracja może dodać wybór klienta w UI).

### ebanking (`ebanking.page.ts`)

| pole TS (`EbankingUser`) | backend |
|---|---|
| `id` | `ebanking_user.id` |
| `username` | `ebanking_user.username` |
| `customer` | `customer.full_name` |
| `channel` | `ebanking_user.channel` |
| `accessStatus` | `ebanking_user.access_status` |
| `dailyLimit` | `ebanking_user.daily_limit` |
| `lastLogin` | `ebanking_user.last_login` → `yyyy-MM-dd HH:mm` |
| `permissions` | `ebanking_user.permissions` CSV → `string[]` |

### enforcement (`enforcement.page.ts`)

| pole TS (`EnforcementCase`) | backend |
|---|---|
| `id` | `enforcement_case.id` |
| `customer` | `customer.full_name` |
| `bailiff` | `enforcement_case.bailiff` |
| `caseNumber` | `enforcement_case.case_number` |
| `blockedAmount` | `enforcement_case.blocked_amount` |
| `transferredAmount` | `enforcement_case.transferred_amount` (nullable → `0` gdy NULL, zgodne z typem TS `number`, patrz `data_model.md` problem #5) |
| `protectedAmount` | `enforcement_case.protected_amount` |
| `status` | `enforcement_case.status` |

### reporting (`reporting.page.ts`, `reporting-dashboard.page.ts`)

| pole TS (`RegulatoryReport`) | backend |
|---|---|
| `id` | `regulatory_report.id` |
| `recipient` | `regulatory_report.recipient` |
| `name` | `regulatory_report.name` |
| `period` | `regulatory_report.period` |
| `dueDate` | `regulatory_report.due_date` |
| `status` | `regulatory_report.status` |

`report_submission` (historia zgłoszeń do bramy) bez konsumenta na liście — wystawiona pod
`/reporting/{id}/submissions`.

### risk (`risk.page.ts`)

| pole TS (`CollateralEntry`) | backend |
|---|---|
| `id` | `collateral_entry.id` |
| `customer` | `customer.full_name` |
| `type` | `collateral_entry.type` |
| `valuation` | `collateral_entry.valuation` |
| `ltv` | `collateral_entry.ltv` |
| `rating` | `collateral_entry.rating` |
| `stage` | `collateral_entry.stage` |
| `provision` | `collateral_entry.provision` |

### dashboard (`dashboard.page.ts`)

`RECENT_CASES`/`REGULATORY_ALERTS` — zapytania agregujące, NIE tabele (`data_model.md`: „to są
zapytania (TOP N po `registered_at`/`due_date`), NIE tabele"). Wystawione pod `/dashboard/recent-cases`
(TOP 4 `customer_case` po `registered_at` desc) i `/dashboard/regulatory-alerts` (TOP 3
`regulatory_report` ze statusem `!= 'accepted'`, po `due_date` asc) — pola spłaszczone identycznie
jak w `cases`/`reporting` wyżej. Kosmetyczny GAP: TOP 4 wg `registered_at desc` nie daje identycznej
listy/kolejności co statyczny `dashboard.data.ts` (`RECENT_CASES` tam to `CS-10231/10229/10225/10219`
— zestaw z frontendu niezwiązany z `registered_at`) — backend zwraca realny TOP N z bazy, co jest
zgodne z intencją „zapytanie agregujące”, a nie z przepisaniem zawartości fixture'a 1:1.

KPI kafelki (`app-kpi-card`) i wykresy (`trendData`, `productData`) są dziś hardkodowanymi literałami
w `dashboard.page.ts` bez importu z `.data.ts` i bez żadnego konsumenta backendu obecnie. Backend
wystawia `GET /dashboard/kpi-snapshot/latest` (najnowszy wiersz `dashboard_kpi_snapshot` po
`snapshot_date`) i `GET /dashboard/portfolio-trend` (wszystkie `portfolio_trend_point`, sort po
`snapshot_date`), bo backend ma do tych danych dostęp — **ale `dashboard.page.ts` NIE jest przepisany
na ich konsumowanie** w tej iteracji: zamiana literałów na dynamiczne wykresy wymagałaby zmiany
struktury widoku (inny zestaw miesięcy/kategorii niż w migawce, inny typ wykresu), co wykracza poza
„zasilenie istniejących widoków danymi" zgodnie z briefem zadania. Endpointy są gotowe do podłączenia
w przyszłej iteracji.

### Pozostałe encje bez widoku-konsumenta (wystawione bo backend ma do nich dostęp)

`employee`, `bank_account`, `chargeback_ref`, `collections_action`, `report_submission`,
`loan_stage_transition` — wystawione pod endpointami pomocniczymi (patrz niżej), bez dedykowanego
widoku frontendowego dziś.

## Endpointy

### Loans

- `GET /loans` → `LoanSummaryDto[]`
- `GET /loans/{id}` → `LoanDetailDto` (404 jeśli brak) — zawiera `installments[]` i `stageTransitions[]`

```json
// LoanSummaryDto
{
  "id": "LN-80231",
  "applicant": "Jan Wiśniewski",
  "segment": "retail",
  "product": "mortgage",
  "stage": "contract",
  "requestedAmount": 480000.00,
  "approvedAmount": 460000.00,
  "scoreResult": 782,
  "dsti": 0.340,
  "createdAt": "2026-08-12"
}
```

```json
// LoanDetailDto (LoanSummaryDto + ...)
{
  "...": "pola LoanSummaryDto",
  "interestRate": 0.0790,
  "termMonths": 12,
  "installments": [
    { "no": 1, "dueDate": "2026-04-15", "principal": 1833.33, "interest": 120.42, "status": "paid" }
  ],
  "stageTransitions": [
    { "fromStage": "application", "toStage": "scoring", "transitionedAt": "2026-03-16T09:00:00Z", "transitionedBy": "M. Wróbel", "approvalId": null }
  ]
}
```

### Cards

- `GET /cards` → `PaymentCardDto[]`
- `GET /cards/authorizations` → `CardAuthorizationDto[]`
- `GET /cards/chargebacks` → `ChargebackDto[]`

### Cases

- `GET /cases` → `CustomerCaseDto[]`

### Collections

- `GET /collections` → `CollectionsCaseDto[]`
- `GET /collections/{id}/actions` → `CollectionsActionDto[]` (404 jeśli sprawa nieznana)

### Contact center

- `GET /contact-center/dispositions` → `ServiceDispositionDto[]`
- `GET /contact-center/history` → `ContactHistoryEntryDto[]`
- `GET /contact-center/profile/{customerId}` → `CustomerProfileDto` (404 jeśli klient nieznany)

### Ebanking

- `GET /ebanking/users` → `EbankingUserDto[]`

### Enforcement

- `GET /enforcement` → `EnforcementCaseDto[]`

### Reporting

- `GET /reporting/reports` → `RegulatoryReportDto[]`
- `GET /reporting/reports/{id}/submissions` → `ReportSubmissionDto[]` (404 jeśli raport nieznany)

### Risk

- `GET /risk/collateral` → `CollateralEntryDto[]`

### Dashboard

- `GET /dashboard/recent-cases` → `RecentCaseDto[]` (TOP 4)
- `GET /dashboard/regulatory-alerts` → `RegulatoryAlertDto[]` (TOP 3)
- `GET /dashboard/kpi-snapshot/latest` → `DashboardKpiSnapshotDto` (404 jeśli brak danych)
- `GET /dashboard/portfolio-trend` → `PortfolioTrendPointDto[]`

### Reference (bez widoku-konsumenta, pomocnicze)

- `GET /customers` → `CustomerDto[]`
- `GET /employees` → `EmployeeDto[]`
- `GET /customers/{id}/accounts` → `BankAccountDto[]` (404 jeśli klient nieznany)

## CORS

Origin konfigurowalny przez `app.cors.allowed-origin` (zmienna środowiskowa o tej samej nazwie),
domyślnie `http://localhost:10500`. Metody: `GET` (jedyne wystawione w tej iteracji).
