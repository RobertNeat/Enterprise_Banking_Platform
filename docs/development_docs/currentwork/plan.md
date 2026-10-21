## Do zrobienia

Kolejność wg zwrotu z inwestycji (wartość funkcjonalna / nakład). Zakres: rozszerzenia istniejących modułów, bez nowych serwisów i bez refaktoru architektury.

1. **Synchronizacja dokumentacji ze stanem kodu** (`docs/development_docs/services/banking_backend.md` pkt 13 i sekcja F6, `services/external_institutions_gateway_mock.md` „Seedowanie startowe”, `banking_backend/data_model.md` sekcja DORA): poprawić opisy na stan bieżący. Seed `card_processor_mock` zna UUID autoryzacji i chargebacku z `003-card.sql`, seedowane `enforcement_case` mają `gateway_enforcement_id` zgodne z seedem bramy, DSH-05 jest już podłączone do `GET /ict-incidents`, a `block`/`unblock`/`chargebacks` w `card_processor_mock` respektują `simulate-outage` (sekcja F4 w `banking_backend.md`). ROI: usuwa fałszywe „braki”, które kierują kolejne prace w złą stronę.
2. **Lint w modułach bez lintera** (`banking_web` — skrypt `lint` z `angular-eslint`; `banking_backend` — Checkstyle lub Spotless w `pom.xml`): wymaga akceptacji użytkownika co do wyboru narzędzia (zasada 3). ROI: bez tego nie da się wykonać kroku weryfikacji z zasady 2 dla dwóch głównych modułów. Nie daje wartości funkcjonalnej, dlatego jest na końcu listy.

Poza zakresem tej iteracji (nakład znacznie powyżej 20%): trwały storage w serwisach mockowych (`ConcurrentHashMap` → JPA), wspólna klasa bazowa klientów REST (`*Client` + `*UnavailableException`→`503`), filtr `status`/`severity` w `governance_mock` `GET /ict-incidents` (filtrowanie robi już `DashboardService`).
