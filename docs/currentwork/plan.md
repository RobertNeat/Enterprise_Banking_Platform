# Testy praktyczne UI (banking_web) — w toku

Cel: przejść przez wszystkie funkcje banking_web przez realne klikanie w przeglądarce
(Claude in Chrome), naprawiając znalezione niedociągnięcia na bieżąco. Pełne testy
integracyjne przepływów między systemami — później, osobny etap.

## Zrobione w tej turze

- Naprawiono: `RolePermissions.java` — ADMIN nie miał uprawnienia `enforcement`, więc
  link w nawigacji był niewidoczny mimo że strona była dostępna pod URL.
- Naprawiono: `shell.page.ts` — nagłówek pokazywał zahardkodowane "Jan Kowalski"
  zamiast `employeeId` z zalogowanej sesji.
- Naprawiono: `loan-detail.page.ts` — harmonogram spłat spadał na syntetycznie
  wygenerowane raty (`buildSchedule`) gdy backend zwrócił pustą tablicę `installments`
  (np. kredyt w stage `application`), pokazując fikcyjne "Paid"/"Upcoming" raty dla
  kredytu, który nigdy nie miał zatwierdzonego finansowania. Dodano test regresji.
- Zweryfikowano przez UI (dwa konta, pełny maker-checker): F2 kredyty — nowy wniosek
  (PRC-20456) → zatwierdzenie w kolejce approvals (PRC-00001) → advance-stage
  decision→contract z realnym harmonogramem.
- Zweryfikowano przez UI: F6 enforcement (lista, dane), F1 dashboard, nawigacja
  filtrowana per rola (risk analyst widzi tylko 3 linki, admin wszystkie).
- Zweryfikowano przez UI: F4 payment cards (file chargeback z realnym reason code),
  F3 credit risk (recalculate ECL — IFRS9 stage/provision się realnie przeliczają),
  F5 e-banking (pełny flow odblokowania z OTP/SCA przez identity_access_mock),
  Contact Channel (wysyłka wiadomości czatu).

## Znana usterka środowiska (nie kod aplikacji)

- Seedy SQL z datami zakotwiczonymi na stałe "now" (np. `006-cases.sql`: "now anchor
  = 2026-10-05") są już nieaktualne względem bieżącej daty systemowej (2026-10-16+).
  Efekt: Case Management → "SLA remaining" pokazuje zawsze `—` (wszystkie
  `sla_due_at` są w przeszłości). Backend (`CaseService.toDto`) liczy to poprawnie
  (`Math.max(0, Duration.between(now, slaDueAt))`) — to nie jest błąd kodu, tylko
  spróchniały seed. Użytkownik zdecydował: zostawić, nie naprawiać teraz.

- Zweryfikowano przez UI: F7 reporting (submit draft→submitted, check-status z
  realnym REJECTED→draft), Case Management (New — potwierdza że SLA liczy się
  poprawnie dla świeżych spraw: 72h dla deadline za 30 dni, vs. `—` dla starych
  seedowanych spraw z przeterminowanym "now anchor"; Escalate), Collections
  (Propose settlement).

- Register — formularz renderuje się poprawnie (nie utworzono testowego konta, by
  nie zaśmiecać stanu demo).
- Weryfikacja końcowa: `banking_web` (`pnpm run check` — format:check + typecheck +
  79/79 testów) i `identity_access_mock` (`mvnw test` — 20/20) przechodzą czysto.

## Do zrobienia

- [ ] Pełne testy integracyjne przepływów między systemami (osobny, późniejszy etap
      — poza zakresem tej rundy UI)
