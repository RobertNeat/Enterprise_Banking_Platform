# F6 — zajęcia komornicze: rejestracja → maker-checker → transfer

Konto: `collections` / `PRC-30678` (permission `enforcement`) dla rejestracji; `admin` NIE widzi
przycisku "Wyślij do zatwierdzenia" (seniority-gate na inicjowaniu maker-checker), ale rejestrację
widzi każdy z dostępem do sekcji.

## Dane z seeda

| Zajęcie | Klient | Status | `kind` | `approval_id` |
| --- | --- | --- | --- | --- |
| `EN-2201` | CUST-006 | `active` | NON_ALIMONY | `44444444-...-001` (`APPROVED`) |
| `EN-2198` | CUST-004 | `settled` | NON_ALIMONY | — |
| `EN-2205` | — (sprawdź na liście) | `registered` | NON_ALIMONY | brak |

## Scenariusz 1 — rejestracja nowego zajęcia (NON_ALIMONY, w limicie)

1. Przejdź do `/enforcement`, kliknij "Nowy".
2. Krok 1: `customer=CUST-002`, `bailiff=Komornik Sądowy J. Nowak`, `caseNumber=KM 2200/26`.
3. Krok 2: `kind=NON_ALIMONY`, `blockedAmount=5000`, `protectedAmount=1631.40` (wartość domyślna —
   w limicie `3604.50` dla NON_ALIMONY).
4. Zatwierdź — `POST /enforcement` → `POST /enforcement/incoming` do bramy.

**Oczekiwany wynik**: nowe zajęcie `status='registered'`, `gatewayEnforcementId` wypełniony
(realne powiązanie z `external_institutions_gateway_mock`, widoczne też na
`http://localhost:10523/ui/enforcement`).

## Scenariusz 2 — rejestracja zajęcia alimentacyjnego

1. Jak wyżej, ale `kind=ALIMONY`, `protectedAmount=0.00` (jedyna dozwolona wartość dla ALIMONY w
   tej iteracji).

**Oczekiwany wynik**: `201`, zajęcie zarejestrowane.

## Scenariusz negatywny 1 — `protectedAmount` nad limitem NON_ALIMONY

1. `kind=NON_ALIMONY`, `protectedAmount=5000.00` (> 3604.50).

**Oczekiwany wynik**: `400 Bad Request` (walidacja klienta blokuje submit; jeśli ominięta, backend
odrzuca niezależnie).

## Scenariusz negatywny 2 — `protectedAmount` > 0 dla ALIMONY

1. `kind=ALIMONY`, `protectedAmount=100.00`.

**Oczekiwany wynik**: `400 Bad Request` — limit ALIMONY jest `0.00`.

## Scenariusz 3 — wysłanie do zatwierdzenia transferu (`EN-2205`, `registered`)

1. Zaloguj się jako `collections` (nie `admin`), znajdź `EN-2205` (`registered`).
2. Kliknij "Wyślij do zatwierdzenia".

**Oczekiwany wynik**: nowy approval `ENFORCEMENT_TRANSFER` w `/approvals` (`PENDING`,
`requestedBy=PRC-30678`), `approval_id` zapisany na zajęciu.

## Scenariusz 4 — transfer po zatwierdzeniu (`EN-2201`, już `APPROVED`)

1. Zdecyduj approval ze scenariusza 3 w `/approvals` jako `admin`/`compliance` (zatwierdź).
2. Na zajęciu z kroku 3 (teraz approval `APPROVED`) otwórz dialog "Transfer".
3. `transferredAmount=4500.00` (mniejsze lub równe `blockedAmount`).
4. Zatwierdź — `POST /enforcement/{id}/transfer`, woła bramę.

**Oczekiwany wynik**: `status→'settled'` (sprawdź rzeczywistą nazwę statusu po transferze na
liście), kwota widoczna w szczegółach.

Alternatywnie — `EN-2201` ma approval już `APPROVED` z seeda, można pominąć kroki 1-2 i przejść
prosto do transferu na `EN-2201`.

## Scenariusz negatywny 3 — transfer bez zatwierdzonego approvala

1. Na nowo zarejestrowanym zajęciu (scenariusz 1, brak `approval_id`) spróbuj otworzyć "Transfer"
   bez wcześniejszego "Wyślij do zatwierdzenia".

**Oczekiwany wynik**: `409 Conflict` — wymaga `approvalId` zapisanego i statusu `APPROVED`.

## Scenariusz 503→incydent ICT

Patrz `outage_and_ict_incidents.md` — rejestracja/transfer podczas niedostępności
`external_institutions_gateway_mock`.
