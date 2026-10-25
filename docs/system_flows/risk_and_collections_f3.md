# F3 — ryzyko (ECL) i windykacja

## Przeliczenie ECL (ryzyko)

Konto: `risk` / `PRC-20456`.

**Ważne o formule**: `banking_backend` woła `credit_risk_mock`'s `POST /ecl/classify` z
`exposureAmount = collateral_entry.valuation` **bez przemnożenia przez `ltv`** (zweryfikowane w
`RiskActionsService.reclassify`, linia `creditRiskClient.classifyEcl(id, daysPastDue,
entry.getRating(), entry.getValuation())`). Próg koszyka: `daysPastDue<=30→stage1 (1%)`,
`31-90→stage2 (5%)`, `>90→stage3 (25%)`. Rezerwa = `valuation × stawka`. Pole `ltv` widoczne na
ekranie jest tylko informacyjne dla tego przeliczenia — nie wchodzi do wzoru.

### Dane z seeda

| Pozycja | Klient | `valuation` | Stage seedowany |
| --- | --- | --- | --- |
| `COL-3301` | CUST-001 | 620 000,00 | stage1 |
| `COL-3302` | CUST-003 | 2 500 000,00 | stage2 |
| `COL-3304` | CUST-006 | 410 000,00 | stage3 |

### Scenariusz 1 — przejście do stage2

1. Przejdź do `/risk`, znajdź `COL-3301`, otwórz dialog "Przelicz ECL".
2. `daysPastDue=45`.
3. Zatwierdź.

**Oczekiwany wynik**: `stage→'stage2'`, `provision=31000.00` (620000 × 5%). Wpis "Historia"
`COLLATERAL_RECLASSIFIED` (`stage1 -> stage2`).

### Scenariusz 2 — przejście do stage3

1. Na tej samej pozycji (teraz `stage2`), ponownie "Przelicz ECL", `daysPastDue=95`.

**Oczekiwany wynik**: `stage→'stage3'`, `provision=155000.00` (620000 × 25%).

### Scenariusz 3 — powrót do stage1

1. `daysPastDue=10`.

**Oczekiwany wynik**: `stage→'stage1'`, `provision=6200.00` (620000 × 1%) — klasyfikacja zawsze
nadpisuje poprzednią, nie ma blokady "tylko w jedną stronę".

### Scenariusz negatywny — ujemne `daysPastDue`

1. `daysPastDue=-5`.

**Oczekiwany wynik**: walidacja klienta (`min=0`) blokuje submit; jeśli ominięta, backend/
`credit_risk_mock` odrzuci `400`.

## Windykacja

Konto: `collections` / `PRC-30678`.

### Dane z seeda

| Sprawa | Kredyt | `daysPastDue` | `stage` |
| --- | --- | --- | --- |
| `DBT-6601` | LN-80203 | 92 | `legal` |
| `DBT-6598` | LN-80180 | 61 | `external` |
| `DBT-6590` | LN-80175 | 34 | `settlement` |
| `DBT-6587` | LN-80162 | 15 | `reminder` |
| `DBT-6580` | LN-80120 | 7 | `monitoring` |
| `DBT-6572` | LN-80099 | 48 | `restructuring` |

### Scenariusz 1 — przypomnienie (`remind`)

1. Przejdź do `/collections`, na `DBT-6587` (`reminder`) kliknij "Przypomnienie".

**Oczekiwany wynik**: `200`, `stage` niezmienione, nowy wiersz `CollectionsAction`
(`action_type='REMINDER_SENT'`), audyt `governance_mock`'s `COLLECTIONS_REMINDER_SENT`.

### Scenariusz 2 — propozycja ugody (`propose-settlement`)

1. Na `DBT-6580` (`monitoring`) kliknij "Propozycja ugody".

**Oczekiwany wynik**: `stage→'settlement'`.

### Scenariusz 3 — transfer zewnętrzny (`transfer-external`)

1. Na `DBT-6590` (`settlement`) kliknij "Transfer zewnętrzny".

**Oczekiwany wynik**: `stage→'external'`.

### Scenariusz negatywny 1 — przypomnienie na sprawie `legal`

1. Na `DBT-6601` (`legal`) kliknij "Przypomnienie".

**Oczekiwany wynik**: `409 Conflict` (`toast.actionBlocked` — brak gate'u po stanie w UI, backend
odrzuca).

### Scenariusz negatywny 2 — transfer zewnętrzny na `monitoring`/`reminder`

1. Na `DBT-6580` (jeśli wciąż `monitoring`, przed scenariuszem 2) lub `DBT-6587` (`reminder`)
   kliknij "Transfer zewnętrzny".

**Oczekiwany wynik**: `409 Conflict`.
