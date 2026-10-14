# Kontrakt API — governance_mock

Serwis wewnętrzny (bez portu na hoście), wołany wyłącznie przez `banking_backend`. Port wewnętrzny: `10524` (env `SERVER_PORT`, patrz `deploy/compose.yml`). Health: `GET /actuator/health` → `{"status":"UP"}`.

Zmienna `GOVERNANCE_SEED_MOCK_DATA` (domyślnie `true`) kontroluje seedowanie startowe kolejki zatwierdzeń zgodne z seedem `banking_backend` — szczegóły w `docs/services/governance_mock.md`.

Stan przechowywany wyłącznie w pamięci procesu (`ConcurrentHashMap`) — reset po restarcie serwisu. Brak bazy danych w tej iteracji.

Błędy walidacyjne i konfliktowe zwracają `ProblemDetail` (RFC 9457), np.:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "type i requestedBy sa wymagane"
}
```

## Approval queue (maker-checker)

### `POST /approvals`

Request:

```json
{
  "type": "LOAN_STAGE_TRANSITION",
  "requestedBy": "PRC-00001",
  "payload": "opis co zatwierdzamy"
}
```

- `type`, `requestedBy` — wymagane (400 jeśli brak). `payload` — opcjonalny opis tekstowy.
- Odpowiedź `201 Created`:

```json
{
  "id": "uuid",
  "type": "LOAN_STAGE_TRANSITION",
  "requestedBy": "PRC-00001",
  "payload": "opis co zatwierdzamy",
  "status": "PENDING",
  "createdAt": "2026-10-14T10:00:00Z",
  "decidedBy": null,
  "decidedAt": null
}
```

### `GET /approvals?status=PENDING`

Lista, opcjonalny filtr `status` (`PENDING|APPROVED|REJECTED`). Sortowana po `createdAt`.

### `GET /approvals/{id}`

`404` jeśli nie istnieje.

### `POST /approvals/{id}/decide`

Request:

```json
{ "decision": "APPROVED", "decidedBy": "PRC-00002" }
```

- `decision` (`APPROVED|REJECTED`), `decidedBy` — wymagane (400 jeśli brak).
- `404` jeśli approval nie istnieje.
- `409` jeśli approval nie jest w statusie `PENDING` (już zdecydowany).
- `409` jeśli `decidedBy` jest równe `requestedBy` (porównanie `trim` + ignorowanie wielkości liter) — zasada czworu oczu.
- Sukces: `200 OK`, zwraca zaktualizowany obiekt approval (status `APPROVED`/`REJECTED`, wypełnione `decidedBy`/`decidedAt`).
- **Efekt uboczny**: automatycznie dopisuje wpis do audit log — `actor=decidedBy`, `action=APPROVAL_APPROVED` lub `APPROVAL_REJECTED`, `entityType=APPROVAL`, `entityId=<approvalId>`. Brak dodatkowego wywołania HTTP potrzebnego ze strony wołającego.

## Audit log

### `POST /audit-events`

Request:

```json
{
  "actor": "PRC-00001",
  "action": "CARD_BLOCKED",
  "entityType": "CARD",
  "entityId": "card-123",
  "details": "opcjonalny opis"
}
```

- `actor`, `action`, `entityType`, `entityId` — wymagane (400 jeśli brak). `details` — opcjonalny.
- `201 Created`, zwraca wpis z wygenerowanym `id` i `timestamp` (serwer nadaje, ignoruje klienta).

### `GET /audit-events?entityType=APPROVAL&entityId=uuid`

Lista, opcjonalne filtry `entityType` i `entityId` (dokładne dopasowanie, `entityType` bez rozróżniania wielkości liter). Sortowana po `timestamp`.

## ICT incident registry (DORA)

### `POST /ict-incidents`

Request:

```json
{
  "source": "CARD_PROCESSOR",
  "description": "opis incydentu",
  "severity": "HIGH"
}
```

- `source`, `description`, `severity` (`LOW|MEDIUM|HIGH`) — wymagane (400 jeśli brak).
- `201 Created`, status startowy `OPEN`, `resolvedAt=null`.

### `GET /ict-incidents`

Lista wszystkich incydentów, sortowana po `createdAt`.

### `POST /ict-incidents/{id}/resolve`

- `404` jeśli incydent nie istnieje.
- `409` jeśli incydent jest już `RESOLVED`.
- Sukces: `200 OK`, status `RESOLVED`, wypełnione `resolvedAt`.
- Brak automatycznego wpisu audytowego (poza zakresem tej iteracji — audytowane są tylko decyzje approval queue i auto-eskalacje SLA).

## F8 — SLA / eskalacja spraw klientów

### `POST /cases/{caseId}/sla-check`

Request:

```json
{ "slaRemainingHours": -1 }
```

- `slaRemainingHours` — wymagane, typ liczbowy (null/brak → `400`).
- Jeśli `slaRemainingHours <= 0`: zwraca `{ "caseId": "...", "escalated": true, "auditEventId": "uuid" }` i dopisuje wpis audytowy (`actor=SYSTEM`, `action=AUTO_ESCALATED`, `entityType=CUSTOMER_CASE`, `entityId=caseId`).
- Jeśli `slaRemainingHours > 0`: zwraca `{ "caseId": "...", "escalated": false, "auditEventId": null }`, brak wpisu audytowego.
- Governance_mock nie przechowuje encji sprawy — `banking_backend` jest właścicielem danych sprawy i wywołuje ten endpoint do oceny reguły.
- Brak deduplikacji: wielokrotne wywołanie dla tej samej sprawy przy `slaRemainingHours <= 0` tworzy kolejne wpisy audytowe (patrz `docs/services/governance_mock.md`, sekcja "Do uzupełnienia").
