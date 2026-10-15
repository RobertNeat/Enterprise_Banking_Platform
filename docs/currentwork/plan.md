# Planowane prace — wiązanie akcji banking_web ↔ banking_backend + JWT/role

Cel: `banking_web` w pełni funkcjonalny (akcje mutujące przechodzą przez `banking_backend` do 5
serwisów mockowych), zabezpieczony JWT (wydawanym przez `banking_backend`, role z
`identity_access_mock`).

Plan wieloetapowy R1→R4 jest KOMPLETNY (R1 auth/JWT, R1.5 approvals + prerequisites, R2 F2/F8, R3
F4/F5, R4 F3/F6/F7). Wszystkie funkcje F2-F8 z `docs/banking_web_functions(concise).md` są podłączone
do backendu. Brak aktywnych pozycji planowanych.

Świadome, trwałe ograniczenia demo pozostające poza zakresem (nie zadania "do zrobienia") — opisane
jako stan bieżący w `docs/services/banking_backend.md`, sekcja "Stan bieżący — ograniczenia
świadome": brak powiązania z bramą dla zajęć/raportów seedowanych, harmonogram rat tylko dla 2 z 13
kredytów, rozjazd nawigacji vs permissions `identity_access_mock`.
