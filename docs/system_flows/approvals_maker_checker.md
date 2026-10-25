# Kolejka zatwierdzeń (maker-checker)

Wspólna dla F2 (kredyty) i F6 (zajęcia komornicze) — każdy approval ma `type` =
`LOAN_STAGE_TRANSITION` albo `ENFORCEMENT_TRANSFER`. Reguła wymuszana na backendzie:
`checker.seniorityLevel > requester.seniorityLevel`, `checker != requester`.

Konto do przeglądania/decydowania: `compliance` / `PRC-50123` (`seniorityLevel=2`) albo `admin` /
`PRC-00001` (`seniorityLevel=3`, jedyna rola mogąca zatwierdzić zgłoszenia od `compliance`).

## Dane z seeda gotowe do testu

| Approval ID | Typ | Encja | Status seedowany | `requestedBy` |
| --- | --- | --- | --- | --- |
| `11111111-...-001` | `LOAN_STAGE_TRANSITION` | `LN-80231` | `APPROVED` (już rozstrzygnięty) | `PRC-10234` |
| `22222222-...-002` | `LOAN_STAGE_TRANSITION` | `LN-80255` | `PENDING` | `PRC-20456` (risk) |
| `44444444-...-001` | `ENFORCEMENT_TRANSFER` | `EN-2201` | `APPROVED` (już rozstrzygnięty) | `PRC-30678` |

## Scenariusz 1 — zatwierdzenie oczekującego approvala (`LN-80255`)

1. Zaloguj się jako `compliance`, otwórz `/approvals`, filtr statusu `PENDING`.
2. Znajdź wpis dla `LN-80255` (`requestedBy=PRC-20456`).
3. Kliknij "Zatwierdź".

**Oczekiwany wynik**: status → `APPROVED`, `decidedBy=PRC-50123` (z JWT, nie z formularza —
formularz wysyła tylko `{decision}`). W `governance_mock` dopisany audit event
`APPROVAL_APPROVED`. Wniosek `LN-80255` jest teraz gotowy do "Awansuj etap" w `/loans/LN-80255`
(zob. `loans_f2.md` scenariusz 2).

## Scenariusz 2 — próba samo-zatwierdzenia (self-approval)

1. Zaloguj się jako `risk` / `PRC-20456` (ten sam, który zgłosił `LN-80255`).
2. Otwórz `/approvals` — przycisk "Zatwierdź"/"Odrzuć" dla wpisu `LN-80255` powinien być **ukryty**
   (`canDecide()` sam odfiltrowuje requestera).
3. Żeby wymusić próbę mimo UI-guard, można wysłać żądanie bezpośrednio (np. przez devtools/curl) —
   backend i tak odrzuci: `decidedBy == requestedBy` → `403`.

**Oczekiwany wynik**: `403 Forbidden`, approval pozostaje `PENDING`, brak wpisu audytowego.

## Scenariusz 3 — checker o niewystarczającym `seniorityLevel`

1. Zarejestruj nowy approval maker-checker (np. przez `loans_f2.md` scenariusz 1 jako `loans`), tak
   żeby powstał kolejny wpis `PENDING` z `requestedBy=PRC-10234` (`seniorityLevel=1`).
2. Zaloguj się jako `contact` / `PRC-40890` — rola nie mapuje się na `RoleCode` z hierarchią
   (`seniorityLevel` nieznany) — przycisk decyzji będzie niewidoczny (UI mirror), a próba
   wywołania bezpośredniego zakończy się `403` (fail-closed dla nierozpoznanej roli checkera po
   stronie `decide()`).

**Oczekiwany wynik**: `403 Forbidden`, approval nadal `PENDING`.

## Scenariusz 4 — odrzucenie

1. Zaloguj się jako `admin` lub `compliance`, znajdź dowolny `PENDING` approval.
2. Kliknij "Odrzuć".

**Oczekiwany wynik**: status → `REJECTED`. Dla `LOAN_STAGE_TRANSITION` — powiązany wniosek wraca do
`stage='application'` przy próbie "Awansuj etap" (zob. `loans_f2.md` scenariusz negatywny 2).

## Scenariusz 5 — ponowna decyzja na już rozstrzygniętym approvalu

1. Spróbuj zdecydować ponownie approval `11111111-...-001` (seedowany jako już `APPROVED`).

**Oczekiwany wynik**: `409 Conflict` ("approval already decided").
