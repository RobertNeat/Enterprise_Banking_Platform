# Planowane prace

- Zaprojektować model danych `banking_backend` (encje JPA + schemat Liquibase) odzwierciedlający dane z `apps/banking_web` (frontend) oraz kontrakty 5 serwisów mockowych. Plan encji: `docs/banking_backend/data_model.md`.
- Wygenerować skrypty Liquibase (formatted SQL, master changelog) tworzące schemat w PostgreSQL/H2, z seedowaniem danych mockowych pod kontekstem Liquibase uzależnionym od zmiennej środowiskowej `BANKING_SEED_MOCK_DATA` (domyślnie `true` lokalnie w `.env.example`/`deploy/compose.dev.yml`, brak w produkcyjnym `compose.yml`/`config.env`).
- Przełączyć `spring.jpa.hibernate.ddl-auto` z `update`/`create-drop` na `validate` w `banking_backend` (main + test), żeby Liquibase było jedynym źródłem schematu.
- Dodać encje/repozytoria Spring Data JPA dla tabel własnościowych `banking_backend` (klienci, konta, pracownicy, wnioski kredytowe + harmonogram, karty, zabezpieczenia/ryzyko, sprawy klientów, windykacja, zajęcia komornicze, raporty regulacyjne, e-banking, migawki KPI pulpitu) — serwisy mockowe pozostają właścicielami własnego stanu w pamięci (nie duplikować ich tabel; `banking_backend` przechowuje tylko referencyjne ID z ich odpowiedzi).
- REST kontrolery `banking_backend` i integracja frontendu z bazą — POZA ZAKRESEM tej sesji, do zaplanowania w kolejnej iteracji.
