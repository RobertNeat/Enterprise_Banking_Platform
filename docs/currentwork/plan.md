# Testy praktyczne UI (banking_web)

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
