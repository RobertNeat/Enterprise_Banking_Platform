## Do zrobienia

Kolejność wg zwrotu z inwestycji (wartość funkcjonalna / nakład). Zakres: rozszerzenia istniejących modułów, bez nowych serwisów i bez refaktoru architektury.

1. **Lint w modułach bez lintera** (`banking_web` — skrypt `lint` z `angular-eslint`; `banking_backend` — Checkstyle lub Spotless w `pom.xml`): wymaga akceptacji użytkownika co do wyboru narzędzia (zasada 3). ROI: bez tego nie da się wykonać kroku weryfikacji z zasady 2 dla dwóch głównych modułów. Nie daje wartości funkcjonalnej, dlatego jest na końcu listy.

Poza zakresem tej iteracji (nakład znacznie powyżej 20%): trwały storage w serwisach mockowych (`ConcurrentHashMap` → JPA), wspólna klasa bazowa klientów REST (`*Client` + `*UnavailableException`→`503`), filtr `status`/`severity` w `governance_mock` `GET /ict-incidents` (filtrowanie robi już `DashboardService`).
