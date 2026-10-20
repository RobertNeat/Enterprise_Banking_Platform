# card_processor_mock — kontrakt API

Serwis eksponowany na hoście: `http://localhost:10521`. Wewnątrz sieci compose: `http://card_processor_mock:10521` (nazwa hosta zgodna z nazwą usługi w `deploy/compose.yml`).

Zmienna `CARD_PROCESSOR_SEED_MOCK_DATA` (domyślnie `true`) kontroluje seedowanie startowe (autoryzacje/chargeback/blokada karty zgodne z seedem `banking_backend`) — szczegóły w `docs/services/card_processor_mock.md`.

Wszystkie odpowiedzi błędów walidacji/brakującego zasobu/niedozwolonego przejścia mają postać:

```json
{ "message": "opis błędu" }
```

(Nest dodaje też pola `statusCode` i `error` w standardowym formacie wyjątków HTTP.)

## POST /authorizations

Request:

```json
{
  "cardId": "string (wymagane)",
  "merchant": "string (wymagane)",
  "amount": "number > 0 (wymagane)",
  "mcc": "string, 4 cyfry (wymagane)",
  "processor": "Visa | Mastercard (opcjonalne)"
}
```

Response `201 Created`:

```json
{
  "id": "string (UUID)",
  "cardId": "string",
  "merchant": "string",
  "mcc": "string",
  "amount": "number",
  "date": "ISO-8601 instant",
  "processor": "Visa | Mastercard",
  "approved": "boolean",
  "responseCode": "00 | 05",
  "processorId": "string (PRC-XXXXXXXX)"
}
```

Reguła decyzji: `amount <= 5000` → `approved: true`, `responseCode: "00"`. `amount > 5000` → `approved: false`, `responseCode: "05"`.

Jeśli `processor` nie podany, wyznaczany deterministycznie z hasha `cardId` (ten sam `cardId` → ten sam `processor` przy każdym wywołaniu).

Błędy: `400 Bad Request` przy brakującym/niepoprawnym `cardId`/`merchant`, `amount` <= 0, `mcc` niezgodnym z `^\d{4}$`, lub `processor` poza `Visa`/`Mastercard`. `503 Service Unavailable` z `{ "message": "Processor unavailable" }` gdy aktywna jest symulacja niedostępności procesora (patrz `POST /processor/simulate-outage`).

## GET /authorizations?cardId=...

Response `200 OK` — lista (może być pusta), elementy w formacie jak w `/authorizations`. Parametr `cardId` opcjonalny — bez niego zwraca wszystkie autoryzacje.

## GET /authorizations/{id}

Response `200 OK` — jak wyżej. `404 Not Found` jeśli autoryzacja nieznana.

## POST /cards/{cardId}/block

Response `200 OK`:

```json
{ "cardId": "string", "blocked": true }
```

`503 Service Unavailable` z `{ "message": "Processor unavailable" }` gdy aktywna jest symulacja
niedostępności procesora (patrz `POST /processor/simulate-outage`).

## POST /cards/{cardId}/unblock

Response `200 OK`:

```json
{ "cardId": "string", "blocked": false }
```

`503 Service Unavailable` jak przy `/block`, gdy aktywna jest symulacja niedostępności procesora.

## GET /cards/{cardId}/block-status

Response `200 OK`:

```json
{ "cardId": "string", "blocked": "boolean" }
```

Domyślnie `blocked: false` dla nieznanego `cardId` (brak wpisu == brak blokady).

## POST /chargebacks

Request:

```json
{
  "authorizationId": "string (wymagane, musi istnieć)",
  "reasonCode": "4855 | 4837 | 4853 | 4863 (wymagane)",
  "description": "string (wymagane)"
}
```

Kody przyczyn:

| reasonCode | znaczenie                                        |
| ---------- | ------------------------------------------------- |
| `4855`     | Towar/usługa niedostarczona                       |
| `4837`     | Transakcja nieautoryzowana (karta nieobecna)      |
| `4853`     | Towar/usługa niezgodna z opisem                   |
| `4863`     | Transakcja sporna — karta obecna, nieuznana przez posiadacza |

Response `201 Created`:

```json
{
  "id": "string (UUID)",
  "authorizationId": "string",
  "reasonCode": "4855 | 4837 | 4853 | 4863",
  "description": "string",
  "status": "FILED",
  "filedAt": "ISO-8601 instant"
}
```

Błędy: `400 Bad Request` przy brakującym `authorizationId`/`description` lub nieznanym `reasonCode`. `404 Not Found` jeśli `authorizationId` nie istnieje. `503 Service Unavailable` gdy aktywna jest symulacja niedostępności procesora (patrz `POST /processor/simulate-outage`).

## POST /chargebacks/{id}/advance

Request:

```json
{ "status": "UNDER_REVIEW | RESOLVED_CUSTOMER | RESOLVED_MERCHANT" }
```

Response `200 OK` — zaktualizowany wpis chargeback (format jak w `POST /chargebacks`).

Dozwolone przejścia cyklu:

| ze stanu        | do stanu                              |
| ---------------- | -------------------------------------- |
| `FILED`          | `UNDER_REVIEW`                         |
| `UNDER_REVIEW`   | `RESOLVED_CUSTOMER`, `RESOLVED_MERCHANT` |
| `RESOLVED_*`      | (brak — stan końcowy)                  |

Błędy: `400 Bad Request` przy nieznanej wartości `status`. `404 Not Found` jeśli chargeback nieznany. `409 Conflict` przy niedozwolonym przejściu (np. pominięcie `UNDER_REVIEW`, zmiana ze stanu `RESOLVED_*`). `503 Service Unavailable` gdy aktywna jest symulacja niedostępności procesora.

## GET /chargebacks?authorizationId=...

Response `200 OK` — lista (może być pusta). Parametr `authorizationId` opcjonalny.

## GET /chargebacks/{id}

Response `200 OK` — jak w `POST /chargebacks`. `404 Not Found` jeśli nieznany.

## POST /processor/simulate-outage

Request:

```json
{ "enabled": "boolean (wymagane)" }
```

Response `200 OK`:

```json
{ "outageSimulated": "boolean" }
```

## GET /processor/status

Response `200 OK`:

```json
{ "outageSimulated": "boolean" }
```
