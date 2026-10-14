# Planowane prace — wiązanie akcji banking_web ↔ banking_backend + JWT/role

Cel: `banking_web` w pełni funkcjonalny (akcje mutujące przechodzą przez `banking_backend` do 5
serwisów mockowych), zabezpieczony JWT (wydawanym przez `banking_backend`, role z
`identity_access_mock`).

Każda runda = agent implementujący → PAUZA (przegląd użytkownika) → agent weryfikujący (tylko
raport, nie modyfikuje kodu) → PAUZA (decyzja użytkownika o następnej rundzie).

## Decyzje użytkownika (rozstrzygnięte przed R2)

- **Trasa ruchu**: potwierdzone — `banking_backend` jest wyłącznym pośrednikiem między `banking_web`
  i 5 serwisami mockowymi. `banking_web` nie woła mocków bezpośrednio nigdzie (R1 już to ustawiło
  dla auth, R1.5 dla approvals; R2+ kontynuuje dla pozostałych flow).

## R3 — F4 (karty) + F5 (e-banking/OTP)

- Karty: autoryzacja (`card_processor_mock`), blokada/odblokowanie, zgłoszenie i cykl chargebacku,
  ścieżka 503→incydent ICT w `governance_mock`.
- E-banking: odblokowanie dostępu z OTP (`identity_access_mock`), reset urządzenia.

## R4 — F3 (ryzyko/windykacja) + F6 (zajęcia komornicze) + F7 (raportowanie)

- F3: przeliczenie ECL (`credit_risk_mock`), zmiana etapu windykacji + audyt.
- F6: rejestracja zajęcia, zatwierdzenie (maker-checker) + przekazanie środków
  (`external_institutions_gateway_mock`).
- F7: generowanie i złożenie raportu regulacyjnego, status przyjęcia/odrzucenia.
