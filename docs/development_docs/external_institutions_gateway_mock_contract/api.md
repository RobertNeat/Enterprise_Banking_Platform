# Kontrakt API — external_institutions_gateway_mock

Port `10523` eksponowany na hoście wyłącznie dla jego stron `/ui/**` (podgląd rejestru/eksportów do debugowania). REST (`/enforcement/**`, `/fatca-crs/**`, `/regulatory-gateway/**`) jest wołany wyłącznie przez `banking_backend` — `banking_web` nie woła tego serwisu bezpośrednio (ustalone w R1.5, `banking_backend` jest wyłącznym pośrednikiem między `banking_web` i serwisami mockowymi). Port: `10523` (env `SERVER_PORT`, patrz `deploy/compose.yml`). Health: `GET /actuator/health` → `{"status":"UP"}`.

Zmienna `GATEWAY_SEED_MOCK_DATA` (domyślnie `true`) kontroluje seedowanie startowe rejestru zajęć komorniczych i zgłoszeń sprawozdawczych zgodne z seedem `banking_backend` — szczegóły w `docs/development_docs/services/external_institutions_gateway_mock.md`.

Stan trwały w dedykowanej instancji PostgreSQL (schema `gateway`, Spring Data JPA + Liquibase) — przetrwa restart serwisu. Szczegóły w `docs/development_docs/services/external_institutions_gateway_mock.md`.

CORS: endpointy REST (`/enforcement/**`, `/fatca-crs/**`, `/regulatory-gateway/**`) mają włączony CORS dla originów z `gateway.cors.allowed-origin-patterns` (domyślnie `http://localhost:*,http://127.0.0.1:*`), metody `GET`/`POST`. Strony `/ui/**` nie wymagają CORS (renderowane serwerowo).

Błędy walidacyjne/konfliktowe (400/404/409) zwracane jako `ProblemDetail` (RFC 9457).

## E-zajęcia komornicze (e-bailiff gateway)

### `POST /enforcement/incoming`

Symuluje odebranie tytułu wykonawczego drogą elektroniczną (od komornika).

Request:

```json
{
  "bailiff": "Komornik Jan Kowalski",
  "caseNumber": "KM 123/26",
  "customer": "CUST-001",
  "blockedAmount": 5000.00,
  "protectedAmount": 1200.00
}
```

- `bailiff`, `caseNumber`, `customer`, `blockedAmount` — wymagane (400 jeśli brak). `protectedAmount` — opcjonalny.
- `201 Created`:

```json
{
  "id": "uuid",
  "customer": "CUST-001",
  "bailiff": "Komornik Jan Kowalski",
  "caseNumber": "KM 123/26",
  "blockedAmount": 5000.00,
  "transferredAmount": null,
  "protectedAmount": 1200.00,
  "status": "registered",
  "createdAt": "2026-10-14T10:00:00Z",
  "settledAt": null
}
```

- `status` — jedna z `registered|active|settled|closed` (serializowane lowercase).

### `GET /enforcement`

Lista wszystkich zajęć (skrzynka odbiorcza komunikatów e-zajęć), sortowana po `createdAt`. Do odpytania przez `banking_backend`.

### `POST /enforcement/{id}/transfer`

Przekazanie zablokowanych środków do komornika — rozlicza zajęcie.

Request:

```json
{ "transferredAmount": 3800.00 }
```

- `transferredAmount` — wymagane, musi być > 0 (400 w przeciwnym razie).
- `404` jeśli zajęcie nie istnieje.
- `409` jeśli zajęcie jest już `settled` lub `closed`.
- Sukces `200 OK`:

```json
{ "id": "uuid", "status": "settled", "confirmedAt": "2026-10-14T10:05:00Z" }
```

## FATCA/CRS export

### `POST /fatca-crs/export`

Request:

```json
{
  "reportId": "RPT-1",
  "period": "2025Q4",
  "recipient": "FATCA/CRS",
  "records": [
    { "customer": "Kowalski & Sons", "taxId": "PL1234567890", "balance": 1000.50 }
  ]
}
```

- `reportId`, `period`, `recipient`, `records` — wymagane (400 jeśli brak). `recipient` musi być dokładnie `"FATCA/CRS"` (400 w przeciwnym razie).
- `records[].customer`, `.taxId`, `.balance` — uproszczony kształt (bez dalszej walidacji).
- `200 OK`:

```json
{
  "reportId": "RPT-1",
  "generatedAt": "2026-10-14T10:00:00Z",
  "xml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<FatcaCrsReport reportId=\"RPT-1\">\n  <Period>2025Q4</Period>\n  <Records>\n    <Record>\n      <Customer>Kowalski &amp; Sons</Customer>\n      <TaxId>PL1234567890</TaxId>\n      <Balance>1000.50</Balance>\n    </Record>\n  </Records>\n</FatcaCrsReport>\n"
}
```

- XML generowany ręcznie (String-builder, bez JAXB) — pola `Customer`/`TaxId` są escapowane (`& < > " '`). Struktura jest uproszczonym mockiem, **nie** zgodna z żadnym konkretnym XSD IRS/OECD — docelowy XSD jest poza zakresem tej iteracji.
- Wygenerowany eksport zapisywany w pamięci (podgląd w UI, `GET /ui/fatca`), kluczowany po `reportId` (ponowny eksport z tym samym `reportId` nadpisuje poprzedni).

## Brama sprawozdawcza KNF/NBP/BFG

### `POST /regulatory-gateway/submit`

Request:

```json
{
  "reportId": "RPT-2",
  "recipient": "KNF",
  "name": "Raport kwartalny",
  "period": "2025Q4"
}
```

- `reportId`, `recipient`, `name`, `period` — wymagane (400 jeśli brak).
- `recipient` musi być jednym z `KNF|NBP|BFG|FATCA/CRS` (400 w przeciwnym razie).
- Odpowiedź natychmiastowa `202 Accepted`:

```json
{
  "submissionId": "uuid",
  "reportId": "RPT-2",
  "recipient": "KNF",
  "name": "Raport kwartalny",
  "period": "2025Q4",
  "status": "PENDING",
  "validationErrors": [],
  "submittedAt": "2026-10-14T10:00:00Z",
  "resolvedAt": null
}
```

- W tle (po `gateway.submission.delay-ms`, domyślnie 2500 ms) status zmienia się na `ACCEPTED` lub `REJECTED`.
- **Reguła deterministyczna** (nie prawdziwy random): `REJECTED` gdy `Math.floorMod(reportId.hashCode(), 10) == 0`, inaczej `ACCEPTED` (~90% przypadków `ACCEPTED` dla losowych `reportId`). Pozwala to wołającemu celowo wywołać znany wynik poprzez wybór `reportId`.
- Gdy `REJECTED`: `validationErrors` zawiera co najmniej jeden komunikat (mock, treść stała).

### `GET /regulatory-gateway/submissions/{submissionId}`

Sprawdzenie aktualnego statusu zgłoszenia (polling).

- `404` jeśli `submissionId` nie istnieje.
- `200 OK` — zwraca obiekt zgłoszenia (jak wyżej), `status` ∈ `PENDING|ACCEPTED|REJECTED`.

Uwaga: status zgłoszenia (`PENDING|ACCEPTED|REJECTED`) to odpowiedź bramy, osobny kontrakt od statusu raportu w `banking_backend` (`draft|submitted|accepted` dla `RegulatoryReport`).
