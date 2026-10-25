# F4 — karty: blokada/odblokowanie, chargeback

Konto: `contact` / `PRC-40890` (ma permission `cards`) albo `admin`.

**Uwaga o trwałości**: `card_processor_mock` trzyma stan tylko w pamięci procesu — po restarcie
kontenera wraca do seeda (`CARD_PROCESSOR_SEED_MOCK_DATA=true`). Chargebacki utworzone w testach
manualnych **nie przetrwają restartu** tego kontenera, a próba `advance` na nieznanym już `id`
zwróci `404`.

## Dane z seeda

| Karta | Klient | Status | Autoryzacje dostępne do chargebacku |
| --- | --- | --- | --- |
| `CRD-5521` | CUST-001 | `active` | `dfd92a8d-...` (Empik), `fc936c4c-...` (Orlen) |
| `CRD-5522` | CUST-002 | `blocked` | `c78bf1a6-...` (Biedronka) |
| `CRD-5530` | CUST-003 | `active` | `449084f7-...` (Amazon EU, już chargebacked: `d9a633a4-...`, `FILED`) |
| `CRD-5541` | CUST-004 | `pending` | — |
| `CRD-5498` | CUST-006 | `expired` | — |

## Scenariusz 1 — blokada karty aktywnej

1. Przejdź do `/cards`, znajdź `CRD-5521` (`active`).
2. Kliknij przycisk blokady.

**Oczekiwany wynik**: `status→'blocked'`, `POST /cards/CRD-5521/block` wołane do
`card_processor_mock`, wpis "Historia" pokazuje zmianę statusu.

## Scenariusz 2 — odblokowanie karty zablokowanej

1. Na `CRD-5522` (`blocked`) kliknij przycisk odblokowania.

**Oczekiwany wynik**: `status→'active'`.

## Scenariusz negatywny 1 — blokada karty `pending`/`expired`

1. Na `CRD-5541` (`pending`) lub `CRD-5498` (`expired`) spróbuj zablokować/odblokować.

**Oczekiwany wynik**: `409 Conflict` — blokada wymaga `status='active'`, odblokowanie wymaga
`status='blocked'`; ani `pending` ani `expired` nie spełnia żadnego z tych warunków.

## Scenariusz 3 — zgłoszenie chargebacku

1. Na `CRD-5521` (`active`) otwórz dialog "Zgłoś chargeback".
2. `authorizationId` = wybierz `dfd92a8d-...` (Empik) z listy.
3. `reasonCode=4855` ("towar niedostarczony").
4. `description=Towar nie dostarczony, zamówienie z 2026-10-01`.
5. Zatwierdź.

**Oczekiwany wynik**: nowy chargeback, `status='FILED'`.

## Scenariusz 4 — awans chargebacku: `FILED → UNDER_REVIEW → RESOLVED_CUSTOMER`

Użyj chargebacku ze scenariusza 3 (lub seedowanego `d9a633a4-...` na `449084f7-...`/`CRD-5530`).

1. Awansuj na `UNDER_REVIEW`.
2. Awansuj na `RESOLVED_CUSTOMER` (lub `RESOLVED_MERCHANT`).

**Oczekiwany wynik**: oba przejścia `200`, drugie przejście jest terminalne.

## Scenariusz negatywny 2 — nielegalne przejście chargebacku

1. Na nowym chargebacku `FILED` spróbuj awansować bezpośrednio na `RESOLVED_CUSTOMER` (pomijając
   `UNDER_REVIEW`).

**Oczekiwany wynik**: `409 Conflict`.

## Scenariusz negatywny 3 — chargeback z `authorizationId` nienależącym do karty

1. Na `CRD-5522` spróbuj zgłosić chargeback z `authorizationId=dfd92a8d-...` (to autoryzacja
   `CRD-5521`, nie `CRD-5522`).

**Oczekiwany wynik**: `404 Not Found` — backend sprawdza lokalnie (`card_authorization_ref`), że
`authorizationId` należy do `cardId` z path, PRZED wywołaniem mocka.

## Scenariusz 503→incydent ICT

Patrz `outage_and_ict_incidents.md` — blokada/odblokowanie/chargeback na `card_processor_mock` w
trybie symulowanej awarii.
