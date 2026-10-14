# credit_risk_mock — kontrakt API

Serwis wewnętrzny, bazowy URL wewnątrz sieci compose: `http://credit_risk_mock:10522` (nazwa hosta zgodna z nazwą usługi w `deploy/compose.yml`). Wołany wyłącznie przez `banking_backend`.

Wszystkie odpowiedzi błędów walidacji/brakującego zasobu mają postać:

```json
{ "message": "opis błędu" }
```

## POST /credit-bureau/score

Request:

```json
{
  "applicantId": "string (wymagane)",
  "requestedAmount": "number > 0 (wymagane)",
  "segment": "retail | business (wymagane)"
}
```

Response `200 OK`:

```json
{
  "applicantId": "string",
  "scoreResult": "int 0-100",
  "dsti": "number 0.00-0.99",
  "inquiryId": "string (UUID)",
  "queriedAt": "ISO-8601 instant"
}
```

Błędy: `400 Bad Request` przy brakującym/niepoprawnym `applicantId`, `requestedAmount` <= 0, lub `segment` poza `retail`/`business`.

`scoreResult` jest deterministyczny dla danego `applicantId` (hash). `dsti` jest deterministyczny dla danej pary `applicantId` + `requestedAmount` i rośnie wraz z `requestedAmount`.

## GET /credit-bureau/history/{applicantId}

Response `200 OK` — lista (może być pusta), elementy w formacie identycznym z odpowiedzią `/credit-bureau/score`, w porządku chronologicznym (najstarsze pierwsze):

```json
[
  { "applicantId": "...", "scoreResult": 0, "dsti": 0.0, "inquiryId": "...", "queriedAt": "..." }
]
```

## POST /ecl/classify

Request:

```json
{
  "positionId": "string (wymagane)",
  "daysPastDue": "int >= 0 (wymagane)",
  "customerRating": "string (wymagane, np. BBB, BB-, A-, CCC, A)",
  "exposureAmount": "number > 0 (wymagane)"
}
```

Response `200 OK`:

```json
{
  "positionId": "string",
  "stage": "stage1 | stage2 | stage3",
  "provision": "number (BigDecimal, 2 miejsca po przecinku)",
  "classifiedAt": "ISO-8601 instant"
}
```

Reguła koszyka (na podstawie `daysPastDue`):

| daysPastDue | stage  | rezerwa (% exposureAmount) |
|-------------|--------|----------------------------|
| 0-30        | stage1 | 1%                         |
| 31-90       | stage2 | 5%                         |
| >90         | stage3 | 25%                        |

Błędy: `400 Bad Request` przy brakującym `positionId`, `daysPastDue` < 0, brakującym `customerRating`, lub `exposureAmount` <= 0.

Każde wywołanie nadpisuje poprzednią klasyfikację dla danego `positionId` (przechowywany jest tylko najnowszy wynik).

## GET /ecl/positions/{positionId}

Response `200 OK` — ostatnia klasyfikacja (format jak w `/ecl/classify`).

Błędy: `404 Not Found` jeśli pozycja nie była jeszcze klasyfikowana.
