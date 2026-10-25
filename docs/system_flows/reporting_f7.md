# F7 — raport regulacyjny: złożenie → status przyjęcia/odrzucenia

Konto: `compliance` / `PRC-50123` (permission `compliance`, mapowane na nav "Raportowanie").

Brak formularza tworzenia raportu — raporty istnieją tylko z seeda. Reguła bramy jest
**deterministyczna po `reportId`**: `floorMod(reportId.hashCode(), 10) == 0 → REJECTED`, inaczej
`ACCEPTED`. Po `submit` status widoczny w UI to `submitted` do kliknięcia "Sprawdź status" (brama
rozstrzyga w tle po ok. 2,5 s — `gateway.submission.delay-ms=2500`; jeśli klikniesz "Sprawdź
status" zbyt szybko, dostaniesz jeszcze `PENDING`, trzeba kliknąć ponownie po chwili).

## Wynik reguły dla raportów z seeda (zweryfikowane)

| Raport | Status seedowany | `floorMod(hash,10)` | Wynik po `submit`→rozstrzygnięciu |
| --- | --- | --- | --- |
| `RPT-KNF-001` | `draft` | 2 | **ACCEPTED** |
| `RPT-BFG-003` | `draft` | 0 | **REJECTED** → wraca do `draft` |
| `RPT-KNF-002` | `draft`, termin już przeterminowany (`due_date = dziś-5`) | 3 | **ACCEPTED** |
| `RPT-NBP-014` | `submitted` (brama ją już rozstrzygnęła na `ACCEPTED` przy starcie kontenera bramy) | 1 | już rozstrzygnięty |
| `RPT-FATCA-009` | `accepted` | 0 | znana rozbieżność demo — nie submituj ponownie, patrz `docs/development_docs/services/banking_backend.md` pkt 11 |

## Scenariusz 1 — złożenie raportu, który brama zaakceptuje (`RPT-KNF-001`)

1. Przejdź do `/reporting`, znajdź `RPT-KNF-001` (`draft`).
2. Kliknij "Złóż" → `POST /reporting/reports/{id}/submit`.

**Oczekiwany wynik**: `status→'submitted'`, nowy wpis w "Historia zgłoszeń"
(`gatewayStatus='PENDING'`).

3. Po ~3 sekundach kliknij "Sprawdź status" → `POST /reporting/reports/{id}/refresh-submission-status`.

**Oczekiwany wynik**: `gatewayStatus='ACCEPTED'`, `status→'accepted'`, toast sukcesu.

## Scenariusz 2 — złożenie raportu, który brama odrzuci (`RPT-BFG-003`)

1. Jak wyżej, na `RPT-BFG-003`.
2. "Złóż", poczekaj, "Sprawdź status".

**Oczekiwany wynik**: `gatewayStatus='REJECTED'`, raport wraca do `status='draft'` (decyzja
projektowa — frontend nie ma statusu `rejected`), `validationErrors` z bramy widoczne w dialogu
"Historia zgłoszeń" jako wyjaśnienie. Można złożyć ponownie — reguła jest deterministyczna po
`reportId`, więc **ponowne złożenie `RPT-BFG-003` zawsze skończy się `REJECTED`** (nie błąd, cecha
mocka).

## Scenariusz 3 — sprawdzenie statusu raportu już rozstrzygniętego w tle (`RPT-NBP-014`)

1. `RPT-NBP-014` jest seedowany jako `submitted`. Kliknij "Sprawdź status" bez wcześniejszego
   "Złóż".

**Oczekiwany wynik**: `gatewayStatus='ACCEPTED'` (rozstrzygnięte przez bramę automatycznie przy
starcie jej kontenera — `reschedulePendingSubmissions()`), `status→'accepted'`.

## Scenariusz negatywny 1 — złożenie raportu, który nie jest `draft`

1. Na raporcie już `submitted`/`accepted` kliknij "Złóż" (jeśli przycisk niewidoczny po stanie,
   wywołanie bezpośrednie i tak zwróci błąd).

**Oczekiwany wynik**: `409 Conflict`.

## Scenariusz negatywny 2 — sprawdzenie statusu raportu, który nie jest `submitted`

1. Na raporcie `draft`/`accepted` kliknij "Sprawdź status".

**Oczekiwany wynik**: `409 Conflict`.

## Dodatkowe — eksport CSV i FATCA/CRS

- "Eksportuj CSV" na `/reporting` jest czysto lokalny (filtr `recipientFilter` po stronie klienta),
  nie woła backendu — dobry test regresji UI, nie integracji.
- `POST /fatca-crs/export` nie jest wołany z `banking_web` w tej iteracji (brak modelu rekordów do
  wypełnienia formularza) — podgląd wygenerowanych eksportów FATCA/CRS tylko w panelu bramy
  `http://localhost:10523/ui/fatca`.
