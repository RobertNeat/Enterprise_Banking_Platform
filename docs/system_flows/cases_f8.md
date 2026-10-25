# F8 — sprawy klientów: termin ustawowy, SLA, eskalacja

Konto: dowolne (brak bramki uprawnień na `/cases`) — przykłady używają `contact` / `PRC-40890`.

**Uwaga o seedzie**: `CS-10229` jest seedowany jako `status='inProgress'` z `sla_due_at` już w
przeszłości (`now - 4h`) specjalnie po to, by zademonstrować automatyczną eskalację —
`CaseSlaEscalationScheduler` (cykl `PT5M`, czyli co 5 minut) **sam przełączy ją na `escalated`
(`escalationSource='AUTO_SLA'`) krótko po starcie stosu**, bez żadnej akcji w UI. Jeśli chcesz
przetestować manualne "Rozpocznij"/"Eskaluj"/"Zamknij" na sprawie w stanie `new`, użyj nowo
utworzonej sprawy (scenariusz 1 niżej), nie `CS-10229`.

## Scenariusz 1 — nowa sprawa: reklamacja (termin 30 dni)

1. Przejdź do `/cases`, kliknij "Nowy".
2. Krok 1: `subject=Reklamacja opłaty za przewalutowanie`, `customerId=CUST-001`,
   `caseType=complaint`.
3. Krok 2: `priority=medium`.
4. Zatwierdź — `POST /cases`.

**Oczekiwany wynik**: nowa sprawa, `status='new'`, `statutoryDeadline` = dziś + 30 dni (termin
liczony przez backend, nie z formularza), SLA do eskalacji = 72h od teraz (`priority=medium`).

## Scenariusz 2 — nowa sprawa: restrukturyzacja (termin 60 dni)

1. Jak wyżej, ale `caseType=restructuring_request`, `priority=high`.

**Oczekiwany wynik**: `statutoryDeadline` = dziś + 60 dni (restrukturyzacja = "szczególnie
złożona"), SLA do eskalacji = 24h od teraz (`priority=high`).

## Scenariusz 3 — cykl życia: Rozpocznij → Eskaluj (manualnie) → Zamknij

Użyj sprawy ze scenariusza 1 (`status='new'`).

1. Kliknij "Rozpocznij" → `status→'inProgress'`.
2. Kliknij "Eskaluj" (SLA jeszcze nie minęło — eskalacja manualna) → `status→'escalated'`,
   `escalationSource='MANUAL'`. Backend sam zapisuje audit event `CASE_ESCALATED_MANUALLY` (brak
   wywołania do `governance_mock`'s auto-escalation w tej ścieżce).
3. Kliknij "Zamknij" (resolve) → `status→'resolved'`.

**Oczekiwany wynik**: historia audytowa sprawy pokazuje 3 wpisy: `CASE_STATUS_CHANGED` (new →
inProgress), `CASE_ESCALATED_MANUALLY`, `CASE_STATUS_CHANGED` (escalated → resolved).

## Scenariusz 4 — automatyczna eskalacja SLA (obserwacja, nie akcja)

1. Zostaw stos działający kilka minut po starcie i obserwuj `CS-10229` na `/cases`.

**Oczekiwany wynik**: w ciągu maks. 5 minut (`banking.cases.sla-escalation.interval=PT5M`) sprawa
przejdzie sama na `status='escalated'`, `escalationSource='AUTO_SLA'`, z wpisem audytowym
`AUTO_ESCALATED` pod aktorem `SYSTEM` (widoczne w dialogu "Historia").

## Scenariusz negatywny 1 — "Rozpocznij" na sprawie innej niż `new`

1. Na sprawie już `inProgress`/`escalated`/`resolved` spróbuj kliknąć "Rozpocznij" (jeśli przycisk
   nie jest ukryty po stanie — wywołaj bezpośrednio `POST /cases/{id}/start`).

**Oczekiwany wynik**: `409 Conflict`.

## Scenariusz negatywny 2 — "Zamknij" sprawy już `resolved`

1. Spróbuj zamknąć sprawę ze scenariusza 3 (już `resolved`).

**Oczekiwany wynik**: `409 Conflict` — `resolved` jest statusem terminalnym.

## Scenariusz negatywny 3 — podwójna eskalacja

1. Na sprawie już `escalated` kliknij "Eskaluj" ponownie.

**Oczekiwany wynik**: `409 Conflict` — zapobiega dopisaniu drugiego wpisu audytowego przy
powtórnym kliknięciu.
