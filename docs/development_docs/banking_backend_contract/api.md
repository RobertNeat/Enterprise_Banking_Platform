# banking_backend — kontrakt API

Serwis eksponowany na hoście: `http://localhost:10520`. Wewnątrz sieci compose:
`http://banking_backend:10520` (nazwa hosta zgodna z nazwą usługi w `deploy/compose.yml`). Wołany
bezpośrednio z przeglądarki przez `banking_web` (CORS skonfigurowany w security filter chain, origin
konfigurowalny przez property `app.cors.allowed-origin` / zmienną środowiskową `BANKING_WEB_ORIGIN`,
domyślnie `http://localhost:10500`; metody `GET, POST, PUT, PATCH, DELETE, OPTIONS`, nagłówki
`Authorization, Content-Type`).

Większość endpointów GET-only. Mutujące endpointy wprowadzone etapami: R2 — `POST /loans`,
`POST /loans/{id}/submit-for-approval`, `POST /loans/{id}/advance-stage`, `POST /cases`,
`POST /cases/{id}/escalate` (F2/F8); R3 — `POST /cards/{cardId}/block|unblock`,
`POST /cards/{cardId}/chargebacks`, `POST /cards/chargebacks/{id}/advance` (F4); R4 —
`POST /risk/collateral/{id}/reclassify` (F3), `POST /collections/{id}/remind|propose-settlement|
transfer-external` (F3), `POST /enforcement` + `/request-transfer-approval` + `/transfer` (F6),
`POST /reporting/reports/{id}/submit|refresh-submission-status` (F7) — zob. sekcje niżej.
Serializacja JSON przez Jackson (transytywnie z `spring-boot-starter-web`, bez nowej zależności).
DTO to Java records; encje JPA nigdy nie są serializowane bezpośrednio.

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

## GET /audit-events (proxy do governance_mock)

Proxy do `governance_mock`'s `GET /audit-events?entityType&entityId` — ślad audytowy zapisywany przez
`recordAuditEvent` (approvals/GovernanceClient). Oba query param opcjonalne; gdy puste/`null`,
`GovernanceClient.listAuditEvents` CAŁKOWICIE je pomija w zapytaniu do `governance_mock` (nie wysyła
pustego stringa) — `governance_mock`'s `AuditService.list` porównuje `entityType`/`entityId` przez
`equalsIgnoreCase`/`equals`, więc puste wartości dopasowałyby się do niczego i zwróciły `[]` zamiast
"brak filtra" (ten sam wzorzec co `listApprovals` ze `status`). Backend niczego nie
wzbogaca/filtruje — czysty proxy. Wymaga JWT. `GovernanceUnavailableException` → `503` (jak w
`/approvals`). Zasila dialog „Historia” w widokach szczegółów wniosku, sprawy, karty i zajęcia.

```json
[
  {
    "id": "uuid", "actor": "PRC-10234", "action": "LOAN_STAGE_CHANGED",
    "entityType": "LOAN_APPLICATION", "entityId": "LN-80203",
    "timestamp": "2026-10-10 10:00", "details": "application -> decision"
  }
]
```

`timestamp` jest sformatowany przez backend na `yyyy-MM-dd HH:mm` w strefie `Europe/Warsaw` (ta sama
konwencja co `EbankingUserDto.lastLogin`, zob. `common/BankTimeZone`) — frontend wyświetla string
bez własnej konwersji strefy czasowej.

Wartości `entityType` używane dziś przez producentów zdarzeń (do przekazania z frontendu):
`LOAN_APPLICATION` (wnioski kredytowe), `CUSTOMER_CASE` (sprawy), `PAYMENT_CARD` (karty),
`ENFORCEMENT_CASE` (zajęcia). `entityId` to id encji (np. `loanId`/`caseId`/`cardId`/
`enforcementCaseId`).

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
`loan_stage_transition`) — renderowane jako osobna sekcja (tabela przejść etapów, sortowana po
`transitionedAt`) pod harmonogramem spłat.

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

`collections_action` (historia wezwań/ugód) — `GET /collections/{id}/actions`, panel "Historia akcji
windykacyjnych" w dialogu na liście.

### contact-center (`contact-center.page.ts`)

| pole TS | backend | uwaga |
|---|---|---|
| `ServiceDisposition.id/typeKey/status/date` | `service_disposition` (`type_key`, `status`, `occurred_at`) | |
| `ServiceDisposition.linkedCase` | `service_disposition.customer_case_id` | |
| `ContactHistoryEntry.id/channelKey/summary` | `contact_history_entry` (`channel_key`, `summary`) | |
| `ContactHistoryEntry.date` | `contact_history_entry.occurred_at` | kolumna jest `DATE` (bez godziny) — frontend ma `'2026-10-05 09:12'`; backend zwraca `yyyy-MM-dd` (ucięta godzina, zgodne z typem kolumny, odnotowane jako znana rozbieżność w `data_model.md` pkt 18) |
| `CustomerProfile` | agregat: `customer` + COUNT `loan_application`/`payment_card`/`customer_case` | `riskRating` = `customer.risk_rating`; klient wybrany przez selektor w UI (zasilany `GET /customers`), domyślnie pierwszy klient z listy. Kosmetyczny GAP: `profile.segment` zwraca `customer.segment` (`retail`/`business`), podczas gdy `contact-center.data.ts` miało wolny tekst `'Retail — Premium'` — backend nie przechowuje tej etykiety marketingowej, uznane za drobne/nieistotne dla testów |
| `ChatMessage`/`INITIAL_CHAT` | brak tabeli | poza zakresem — czysto frontendowe (potwierdzone w `data_model.md`) |

`GET /contact-center/dispositions` i `GET /contact-center/history` przyjmują opcjonalny parametr
`customerId` (brak → wszystkie wiersze). Dyspozycje nie mają własnej kolumny `customer_id` —
filtrowanie idzie przez `service_disposition.customer_case_id` → `customer_case.customer_id`.

`PUT /contact-center/dispositions/{id}/case` → `ServiceDispositionDto`. Body: `{ "caseId" }`.
Łączy dyspozycję z inną sprawą (F8). `404` jeśli dyspozycja lub sprawa nieznana; `400` jeśli
`caseId` jest puste lub sprawa należy do innego klienta niż obecna sprawa dyspozycji (ochrona przed
odtworzeniem błędu z seeda opisanego w `services/banking_backend.md`).

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

`report_submission` (historia zgłoszeń do bramy, z `validationErrors`) — `GET /reporting/reports/{id}/submissions`,
panel "Historia zgłoszeń do bramy" w dialogu na liście.

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

`GET /dashboard/ict-incidents` → `IctIncidentAlertDto[]` — proxy do governance_mock's
`GET /ict-incidents` (DORA), TOP 5 najnowszych incydentów ze statusem `OPEN` (filtrowanie i sortowanie
po `createdAt` desc robi `DashboardService`, nie governance_mock). Pola: `id`, `source`, `description`,
`severity` (`LOW|MEDIUM|HIGH`), `createdAt`. `GovernanceUnavailableException` → `503` (jak w `/approvals`).
Konsumowane przez `dashboard.page.ts` w osobnej karcie "Incydenty ICT (DORA)" obok Alertów regulacyjnych.

KPI kafelki (`app-kpi-card`) i wykres trendu (`trendData`) konsumują `GET /dashboard/kpi-snapshot/latest`
(najnowszy wiersz `dashboard_kpi_snapshot` po `snapshot_date`; `404` jeśli brak danych — kafelki wtedy
pokazują placeholder `—`) i `GET /dashboard/portfolio-trend` (wszystkie `portfolio_trend_point`, sort
po `snapshot_date` asc, robi to backend). Wykres "portfel wg produktu" (`productData`) agreguje
`GET /loans` po polu `product` na frontendzie (backend nie wystawia takiej agregacji). Różnica skali
między migawką KPI (dziesiątki tysięcy kredytów) a wykresem produktów (13 wniosków z seeda) jest
zamierzona, zob. `services/banking_backend.md` pkt 21.

### Pozostałe encje bez widoku-konsumenta (wystawione bo backend ma do nich dostęp)

`employee`, `bank_account`, `chargeback_ref`, `collections_action`, `report_submission`,
`loan_stage_transition` — wystawione pod endpointami pomocniczymi (patrz niżej), bez dedykowanego
widoku frontendowego dziś.

## Endpointy

### Loans

- `GET /loans` → `LoanSummaryDto[]`
- `GET /loans/{id}` → `LoanDetailDto` (404 jeśli brak) — zawiera `installments[]` i `stageTransitions[]`
- `POST /loans` (F2) → `LoanDetailDto`. Request: `{ "customerId", "segment", "product", "requestedAmount", "termMonths"? }`
  (bez `scoreResult`/`dsti` — pochodzą z `credit_risk_mock` POST `/credit-bureau/score`, wołane
  przed zapisem; outage mocka → `503`, brak zapisanego wiersza). Zapisuje `stage='application'`,
  `interestRate` = stała 0.0790 (brak tabeli stawek per produkt), `termMonths` domyślnie 36 gdy
  nie podano. `400` jeśli `segment` nie jest `retail`/`business` lub `product` nie jest jednym z
  `cash`/`mortgage`/`overdraft`/`investment`/`creditLine` — walidacja następuje przed wywołaniem
  `credit_risk_mock`, więc błędny request nie zużywa bureau inquiry.
- `POST /loans/{id}/submit-for-approval` (F2, `application`→`decision`) → `{ "loan": LoanDetailDto, "approvalId" }`.
  Otwiera wpis w `governance_mock` (`type=LOAN_STAGE_TRANSITION`, `requestedBy`=employeeId z JWT),
  zapisuje `LoanStageTransition` z `approvalId`, woła `recordAuditEvent(LOAN_STAGE_CHANGED)`. `409`
  jeśli wniosek nie jest w `application`. `403` jeśli wnioskodawca ma najwyższy `seniorityLevel`
  w systemie (`RoleCode.hasHigherSeniorityRole()` = false, dziś tylko `ADMIN`) — nikt nie mógłby
  tego approvala zatwierdzić.
- `POST /loans/{id}/advance-stage` (F2, `decision`→`contract`) → `LoanDetailDto`. Odczytuje
  `approvalId` z ostatniego przejścia do `decision`, wymaga `governance_mock` approval w statusie
  `APPROVED` (`409` jeśli `PENDING`/`REJECTED`). Ustawia `approvedAmount=requestedAmount`, generuje
  `Installment` (rata równa, annuity, `interestRate`/`termMonths` z encji), woła
  `recordAuditEvent(LOAN_STAGE_CHANGED)`. Wywoływane ręcznie przez pracownika po zobaczeniu
  decyzji w kolejce zatwierdzeń (decyzja projektowa R2 — brak automatycznego callbacku z
  `ApprovalsService.decide()`, zob. `docs/services/banking_backend.md`).

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
- `POST /cards/{cardId}/block` (F4) → `PaymentCardDto`. Woła `card_processor_mock` `POST
  /cards/{cardId}/block`, zapisuje `status='blocked'`. `409` jeśli obecny `status != 'active'`.
  Audytuje `CARD_BLOCKED` w `governance_mock`.
- `POST /cards/{cardId}/unblock` (F4) → `PaymentCardDto`. Analogicznie, `status='active'`, `409`
  jeśli obecny `status != 'blocked'`. Audytuje `CARD_UNBLOCKED`.
- `POST /cards/{cardId}/chargebacks` (F4) → `ChargebackDto`. Request: `{ "authorizationId",
  "reasonCode", "description" }`. Woła `card_processor_mock` `POST /chargebacks`, zapisuje
  `ChargebackRef` (id z odpowiedzi mocka). `400` jeśli `reasonCode` poza `4855|4837|4853|4863`
  (walidacja PRZED wywołaniem `card_processor_mock`, zgodna z CHECK constraint na
  `chargeback_ref.reason_code`). `404` jeśli `authorizationId` nieznane lub nie należy do
  `cardId`. Audytuje `CARD_CHARGEBACK_FILED`.
- `POST /cards/chargebacks/{id}/advance` (F4) → `ChargebackDto`. Request: `{ "status" }`
  (`UNDER_REVIEW|RESOLVED_CUSTOMER|RESOLVED_MERCHANT`). Woła `card_processor_mock` `POST
  /chargebacks/{id}/advance`, aktualizuje `status` lokalnie. `404` jeśli chargeback nieznany, `409`
  przekazane 1:1 z mocka przy niedozwolonym przejściu. Audytuje `CARD_CHARGEBACK_ADVANCED`.

**Ścieżka 503 → incydent ICT (F4 alt-flow):** jeśli `card_processor_mock` jest nieosiągalny
(connection failure) LUB zwraca `503` (symulowany outage — `block`/`unblock`/`chargebacks`/
`chargebacks/{id}/advance` w mocku sprawdzają flagę outage i zwracają `503` tak samo jak
`POST /authorizations`) — przed zwróceniem `503` do `banking_web`, backend zgłasza incydent ICT do
`governance_mock` (`POST
/ict-incidents`, `source="CARD_PROCESSOR"`, `severity="HIGH"`). Jeśli to zgłoszenie też zawiedzie
(podwójna awaria), oryginalny błąd `503` i tak wraca do klienta (błąd zgłoszenia logowany, nie
maskuje oryginału). Brak endpointu `POST /cards/{cardId}/authorize` — ocena R3: autoryzacja karty
jest transakcją klienta (terminal/e-commerce), nie akcją pracownika w tym UI; `cards.page.ts` nie
miało i nie ma formularza do ręcznego tworzenia autoryzacji.

### Cases

- `GET /cases` → `CustomerCaseDto[]`
- `POST /cases` (F8) → `CustomerCaseDto`. Request: `{ "subject", "customerId", "ownerId"?, "priority", "caseType" }`
  (bez `statutoryDeadline`/`slaRemainingHours` — wyliczane przez backend, zob. reguła w
  `docs/services/banking_backend.md`). `ownerId` opcjonalny, domyślnie employeeId z JWT. Status
  startowy `new`. `400` jeśli `priority` nie jest `low`/`medium`/`high`, lub `caseType` jest puste
  albo dłuższe niż 30 znaków.
- `POST /cases/{id}/escalate` (F8) → `{ "customerCase": CustomerCaseDto, "escalated": boolean }`.
  Woła `governance_mock` `POST /cases/{caseId}/sla-check` z aktualnym SLA wyliczonym z
  `sla_due_at`. `escalated=true` (SLA już minęło) → status `escalated`, audit event już dopisany
  przez `governance_mock` (brak duplikacji). `escalated=false` (eskalacja manualna mimo że SLA
  jeszcze nie minęło) → status `escalated`, backend dopisuje własny `CASE_ESCALATED_MANUALLY`
  audit event. `409` jeśli sprawa już `escalated` lub `resolved`.

### Collections

- `GET /collections` → `CollectionsCaseDto[]`
- `GET /collections/{id}/actions` → `CollectionsActionDto[]` (404 jeśli sprawa nieznana)
- `POST /collections/{id}/remind` (F3) → `CollectionsCaseDto`. Bez zmiany `stage` — tylko audyt
  (`COLLECTIONS_REMINDER_SENT`) i wpis `CollectionsAction` (`action_type='REMINDER_SENT'`). `409`
  jeśli `stage='legal'` (walidacja przeniesiona z frontendu, R4).
- `POST /collections/{id}/propose-settlement` (F3) → `CollectionsCaseDto`. `stage→'settlement'`,
  audytuje `COLLECTIONS_SETTLEMENT_PROPOSED`, wpis `CollectionsAction`
  (`action_type='SETTLEMENT_PROPOSED'`).
- `POST /collections/{id}/transfer-external` (F3) → `CollectionsCaseDto`. `stage→'external'`, `409`
  jeśli `stage` to `'monitoring'`/`'reminder'` (walidacja przeniesiona z frontendu, R4). Audytuje
  `COLLECTIONS_TRANSFERRED_EXTERNAL`, wpis `CollectionsAction` (`action_type='EXTERNAL_TRANSFER'`).

### Contact center

- `GET /contact-center/dispositions?customerId=` → `ServiceDispositionDto[]` (`customerId` opcjonalny)
- `GET /contact-center/history?customerId=` → `ContactHistoryEntryDto[]` (`customerId` opcjonalny)
- `GET /contact-center/profile/{customerId}` → `CustomerProfileDto` (404 jeśli klient nieznany)
- `PUT /contact-center/dispositions/{id}/case` (F8) → `ServiceDispositionDto`. Request: `{ "caseId" }`.
  `404` jeśli dyspozycja lub sprawa nieznana; `400` jeśli `caseId` puste lub sprawa należy do innego
  klienta niż obecna sprawa dyspozycji.

### Ebanking

- `GET /ebanking/users` → `EbankingUserDto[]`
- `POST /ebanking/{userId}/unlock/request-otp` (F5, krok 1) → `{ "otpId", "expiresAt" }`. Woła
  `identity_access_mock` `POST /otp/request` z `{employeeId: <jwt.sub>, purpose: "UNLOCK_ACCESS"}`.
  `409` jeśli `accessStatus` już `'active'` (walidacja PRZED wywołaniem OTP).
- `POST /ebanking/{userId}/unlock/confirm` (F5, krok 2) → `EbankingUserDto`. Request: `{ "otpId",
  "code" }`. Weryfikuje przez `POST /otp/verify`; `403` jeśli `verified:false` (kod
  błędny/wygasły/zużyty). Jeśli zweryfikowany, woła `POST /ebanking-access/{userId}/unlock`
  (faktycznie konsumuje OTP), ustawia `accessStatus='active'`. `409` jeśli już `'active'` (ta sama
  walidacja jak w request-otp, sprawdzana PRZED weryfikacją kodu). Audytuje
  `EBANKING_ACCESS_UNLOCKED`.
- `GET /ebanking/{userId}/devices` → `EbankingDeviceDto[]`. Proxy do `identity_access_mock` `GET
  /devices/{ownerId}` (`ownerId=userId`, np. `EB-1001`) — pozwala frontendowi wybrać urządzenie do
  resetu.
- `POST /ebanking/{userId}/devices/{deviceId}/reset/request-otp` (F5, reset urządzenia krok 1) →
  `{ "otpId", "expiresAt" }`. Jak unlock, ale `purpose="RESET_DEVICE"`. `404` jeśli `deviceId` nie
  jest na liście urządzeń tego `userId` (walidacja własności przed trustowaniem path param).
- `POST /ebanking/{userId}/devices/{deviceId}/reset/confirm` (F5, reset urządzenia krok 2) → `204`.
  Request: `{ "otpId", "code" }`. Weryfikuje OTP (`403` jeśli `verified:false`), woła `POST
  /devices/{deviceId}/reset` (konsumuje OTP, oznacza urządzenie `trusted:false`). Audytuje
  `DEVICE_RESET`.

OTP/SCA (PSD2) — kod nigdy nie przechodzi przez `banking_backend` ani `banking_web`: widoczny
wyłącznie w panelu Thymeleaf `identity_access_mock:10525/otp` do manualnego testu demo. Pracownik
przekazuje kod klientowi kanałem pozakanałowym (symulowane w UI jako podpowiedź, nie realny SMS).

### Enforcement

- `GET /enforcement` → `EnforcementCaseDto[]`
- `POST /enforcement` (F6) → `EnforcementCaseDto`. Request: `{ "bailiff", "caseNumber", "customer",
  "kind"?, "blockedAmount", "protectedAmount"? }` (BEZ `transferredAmount` — nic nie jest przekazane w
  momencie rejestracji). Woła `external_institutions_gateway_mock` `POST /enforcement/incoming`,
  zapisuje `EnforcementCase` z `status='registered'` i `gatewayEnforcementId` z odpowiedzi bramy.
  Zajęcia seedowane (`db/changelog/seed/009-enforcement.sql`) mają `gatewayEnforcementId` wypełniony
  fixed UUID-ami zgodnymi z bramy własnym seedem (`EnforcementService.seed()`), więc akcje na nich
  działają identycznie jak na nowo zarejestrowanych. Audytuje `ENFORCEMENT_REGISTERED`. `503` +
  incydent ICT (`source="EXTERNAL_INSTITUTIONS_GATEWAY"`) jeśli brama nieosiągalna — ścieżka
  analogiczna do F4 (`card_processor_mock`).
  **ZJC-03** — `kind` (`ALIMONY`\|`NON_ALIMONY`, domyślnie `NON_ALIMONY` gdy pominięte) steruje
  limitem "kwoty wolnej" (`protectedAmount`): `400` gdy `protectedAmount` przekracza limit per `kind`
  lub `kind` jest nierozpoznane, lub `blockedAmount` jest ujemne/brak. Limity są konfigurowalne
  (`banking.enforcement.protected-amount-limit.{non-alimony,alimony}`, patrz `services/banking_backend.md`).
- `POST /enforcement/{id}/request-transfer-approval` (F6, maker-checker) →
  `{ "enforcementCase": EnforcementCaseDto, "approvalId" }`. Otwiera approval w `governance_mock`
  (`type="ENFORCEMENT_TRANSFER"`), zapisuje `approvalId` na encji (kolumna `approval_id`, już
  istniała od R1.5 jako zmiana na encji, bez potrzeby nowej migracji w R4), `status→'active'`. `409`
  jeśli `status != 'registered'` lub `gatewayEnforcementId=NULL` (defensywna gałąź — brak takiego
  przypadku w bieżącym seedzie). `403` jeśli wnioskodawca ma najwyższy `seniorityLevel` w systemie
  (`RoleCode.hasHigherSeniorityRole()` = false, dziś tylko `ADMIN`) — nikt nie mógłby tego approvala
  zatwierdzić.
- `POST /enforcement/{id}/transfer` (F6) → `EnforcementCaseDto`. Request: `{ "transferredAmount" }`.
  Wymaga `status='active'`, `gatewayEnforcementId` nie-NULL, oraz approval zapisany w `approvalId`
  w statusie `APPROVED` w `governance_mock` (wzorzec identyczny z `LoanActionsService.advanceStage`,
  F2) — `409` w każdym innym przypadku. Woła bramę `POST /enforcement/{gatewayEnforcementId}/transfer`,
  `status→'settled'`. Audytuje `ENFORCEMENT_FUNDS_TRANSFERRED`. `503`+incydent ICT jak przy rejestracji.

### Reporting

- `GET /reporting/reports` → `RegulatoryReportDto[]`
- `GET /reporting/reports/{id}/submissions` → `ReportSubmissionDto[]` (404 jeśli raport nieznany)
- `POST /reporting/reports/{id}/submit` (F7) → `RegulatoryReportDto`. Wymaga `status='draft'` (`409`
  inaczej). Woła `external_institutions_gateway_mock` `POST /regulatory-gateway/submit`, zapisuje
  `ReportSubmission` (`gatewayStatus='PENDING'`), `status→'submitted'`. Audytuje `REPORT_SUBMITTED`.
  `503`+incydent ICT jeśli brama nieosiągalna.
- `POST /reporting/reports/{id}/refresh-submission-status` (F7) →
  `{ "report": RegulatoryReportDto, "submission": ReportSubmissionDto }`. Wymaga `status='submitted'`
  (`409` inaczej). Woła `GET /regulatory-gateway/submissions/{gatewaySubmissionId}` na najnowszym
  `ReportSubmission` tego raportu, aktualizuje `gatewayStatus`/`validationErrors`/`resolvedAt`.
  `ACCEPTED` → `RegulatoryReport.status='accepted'`. `REJECTED` → `status='draft'` (model TS
  `reporting.model.ts` nie ma statusu `'rejected'`; `validationErrors` z bramy pozostają widoczne we
  frontendzie nawet po powrocie do `draft` — zob. decyzja projektowa w
  `docs/services/banking_backend.md`). Audytuje `REPORT_SUBMISSION_STATUS_REFRESHED`. Brak endpointu
  `POST /fatca-crs/export` — `/regulatory-gateway/submit` obsługuje `recipient=FATCA/CRS` tą samą
  ścieżką, a `reporting.page.ts` nie rozróżnia FATCA/CRS od innych odbiorców poza filtrem listy.

### Risk

- `GET /risk/collateral` → `CollateralEntryDto[]`
- `POST /risk/collateral/{id}/reclassify` (F3) → `CollateralEntryDto`. Request:
  `{ "daysPastDue" }` (wpisywane przez pracownika — `CollateralEntry` nie ma własnego pola
  opóźnienia i nie ma jednoznacznego łańcucha FK do `CollectionsCase`, zob. decyzja projektowa w
  `docs/services/banking_backend.md`). Woła `credit_risk_mock` `POST /ecl/classify` z
  `positionId=id`, `customerRating=entry.rating`, `exposureAmount=entry.valuation`, zapisuje
  `stage`/`provision`/`eclClassifiedAt`. Audytuje `COLLATERAL_RECLASSIFIED`.

### Dashboard

- `GET /dashboard/recent-cases` → `RecentCaseDto[]` (TOP 4)
- `GET /dashboard/regulatory-alerts` → `RegulatoryAlertDto[]` (TOP 3)
- `GET /dashboard/kpi-snapshot/latest` → `DashboardKpiSnapshotDto` (404 jeśli brak danych)
- `GET /dashboard/portfolio-trend` → `PortfolioTrendPointDto[]`
- `GET /dashboard/ict-incidents` → `IctIncidentAlertDto[]` (TOP 5 `OPEN`, proxy do governance_mock)

### Reference (bez widoku-konsumenta, pomocnicze)

- `GET /customers` → `CustomerDto[]`
- `GET /employees` → `EmployeeDto[]`
- `GET /customers/{id}/accounts` → `BankAccountDto[]` (404 jeśli klient nieznany)

## CORS

Origin konfigurowalny przez `app.cors.allowed-origin` (zmienna środowiskowa o tej samej nazwie),
domyślnie `http://localhost:10500`. Metody: `GET, POST, PUT, PATCH, DELETE, OPTIONS` (security filter
chain pozwala wszystkie od R1; `POST` faktycznie używane od R1.5/R2 dla auth/approvals/loans/cases).
