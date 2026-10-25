# Przepływy danych — testy manualne

Ten katalog opisuje konkretne ścieżki klikalne w `banking_web`, z przykładowymi danymi do wpisania,
żeby można było manualnie zweryfikować każdy mutujący przepływ od formularza przez
`banking_backend` do odpowiedniego mocka i bazy danych. Każdy plik to osobny obszar funkcjonalny;
scenariusze w ramach jednego pliku da się przejść w podanej kolejności.

## Wymagania wstępne

Stos trzeba podnieść samodzielnie (zgodnie z zasadą projektu — kontenery Docker podnosi użytkownik,
nie agent):

```
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env up -d --build
```

Ustaw w `.env` (na podstawie `.env.example`) `BANKING_SEED_MOCK_DATA=true` — bez tego
`banking_backend` startuje z pustym schematem i żaden scenariusz niżej nie zadziała (seed jest
jedynym źródłem kont demo, klientów, kredytów, kart itd.). Pozostałe trzy mocki
(`identity_access_mock`, `governance_mock`, `external_institutions_gateway_mock`) mają własne flagi
seeda domyślnie `true` — nie trzeba ich ustawiać ręcznie, tylko nie wyłączać.

Po podniesieniu stosu aplikacja jest pod `http://localhost:10500`.

## Porty na hoście

| Serwis | Port | Dostęp |
| --- | --- | --- |
| `banking_web` | 10500 | przeglądarka — główna aplikacja |
| `banking_backend` | 10520 | REST, wołany przez `banking_web` |
| `card_processor_mock` | 10521 | REST (brak UI operatorskiego — placeholder) |
| `credit_risk_mock` | — | tylko sieć wewnętrzna, wołany przez `banking_backend` |
| `external_institutions_gateway_mock` | 10523 | panel operatorski Thymeleaf pod `/ui/*` |
| `governance_mock` | — | tylko sieć wewnętrzna, wołany przez `banking_backend` |
| `identity_access_mock` | 10525 | panel operatorski Thymeleaf, w tym `/otp` (kod OTP do odczytu manualnego) |

## Konta demo (`login.page.ts`)

| Login (`id`) | `employeeId` | Hasło | Rola | `seniorityLevel` | Widoczne sekcje nav (permission) |
| --- | --- | --- | --- | --- | --- |
| `admin` | `PRC-00001` | `Admin12345` | Administrator systemu | 3 | wszystkie |
| `loans` | `PRC-10234` | `Loans12345` | Doradca kredytowy | 1 | dashboard, loans, ebanking |
| `risk` | `PRC-20456` | `Risk123456` | Analityk ryzyka | 1 | dashboard, risk, loans |
| `collections` | `PRC-30678` | `Collect1234` | Windykator | 1 | dashboard, collections, enforcement |
| `contact` | `PRC-40890` | `Contact1234` | Konsultant kanału kontaktu | 1 | dashboard, contact, ebanking, cards |
| `compliance` | `PRC-50123` | `Compliance1` | Compliance / AML | 2 | dashboard, compliance (reporting), audit, approvals |

Uwaga: sekcja **Sprawy klientów** (`/cases`) nie ma żadnej bramki uprawnień — widoczna dla każdego
zalogowanego konta. Wszystkie trasy pod `/` wymagają tylko poprawnego JWT (`authGuard`) — link w
menu jest skrywany po permission, ale wpisanie adresu bezpośrednio otwiera stronę niezależnie od
roli; **konkretna akcja mutująca i tak jest weryfikowana po stronie backendu** (np. tylko
`seniorityLevel < 3` może zainicjować maker-checker — przycisk "Nowy" w kredytach i "Wyślij do
zatwierdzenia" w zajęciach komorniczych jest skryty dla `admin`).

## Dodatkowe miejsca weryfikacji (poza `banking_web`)

- **Kod OTP** (odblokowanie e-bankingu / reset urządzenia, F5): widoczny wyłącznie w panelu
  `http://localhost:10525/otp` — `banking_web` nigdy nie pokazuje kodu, trzeba go przepisać ręcznie
  z drugiej karty przeglądarki.
- **Panel zajęć/raportów `external_institutions_gateway_mock`**: `http://localhost:10523/ui/enforcement`,
  `/ui/reports`, `/ui/fatca` — podgląd stanu po stronie bramy (np. XML FATCA/CRS), przydatny do
  potwierdzenia, że wywołanie z `banking_backend` faktycznie doszło do bramy.
- **Historia audytowa** (dialog "Historia" na widokach kredytów/kart/spraw/zajęć w `banking_web`) —
  czyta `governance_mock` przez `GET /audit-events`; nie ma osobnego panelu `governance_mock` (brak
  portu na hoście).
- **Karta "Alerty ICT" na pulpicie** (`/dashboard`) — pokazuje TOP 5 najnowszych incydentów `OPEN`
  zarejestrowanych przez ścieżkę 503→incydent ICT (zob. `outage_and_ict_incidents.md`).

## Reset danych

- `banking_backend` (klienci, kredyty, karty, sprawy, zajęcia, raporty, ryzyko, dashboard) żyje w
  wolumenie `postgres-data`.
- `governance_mock` + `identity_access_mock` + `external_institutions_gateway_mock` (approvale,
  audyt, incydenty ICT, konta, urządzenia, OTP, zajęcia po stronie bramy, zgłoszenia regulacyjne)
  żyją **razem** w jednym wolumenie `mocks-postgres-data`, każdy we własnym schemacie
  (`governance`/`identity_access`/`gateway`) — nie da się zresetować tylko jednego z nich bez
  wejścia do bazy i `DROP SCHEMA <schema> CASCADE`.
- `card_processor_mock` i `credit_risk_mock` trzymają stan tylko w pamięci procesu — restart
  kontenera czyści je do seeda (lub do pustego stanu, zależnie od flagi `*_SEED_MOCK_DATA`).

**`docker compose down -v` usuwa OBA wolumeny Postgres naraz** (dane `banking_backend` razem z
danymi trzech mocków) — nie jest to sposób na zresetowanie tylko jednej części systemu.

## Pliki w tym katalogu

| Plik | Zakres |
| --- | --- |
| `auth_and_registration.md` | logowanie, rejestracja nowego konta (i jej ograniczenie) |
| `loans_f2.md` | F2 — wniosek kredytowy: scoring → zatwierdzenie → decyzja → wypłata |
| `approvals_maker_checker.md` | kolejka zatwierdzeń (maker-checker) — wspólna dla F2/F6 |
| `cases_f8.md` | F8 — sprawy klientów: termin ustawowy, SLA, eskalacja |
| `cards_f4.md` | F4 — karty: blokada/odblokowanie, chargeback |
| `ebanking_f5.md` | F5 — e-banking: odblokowanie i reset urządzenia (OTP) |
| `risk_and_collections_f3.md` | F3 — przeliczenie ECL (ryzyko) i akcje windykacyjne |
| `enforcement_f6.md` | F6 — zajęcia komornicze: rejestracja → maker-checker → transfer |
| `reporting_f7.md` | F7 — raport regulacyjny: złożenie → status przyjęcia/odrzucenia |
| `outage_and_ict_incidents.md` | symulacja niedostępności mocka → incydent ICT (DORA) |

Każdy scenariusz w plikach niżej ma tę samą strukturę: konto do zalogowania, wymagany stan encji z
seeda, kroki, dosłowne wartości do wpisania, oczekiwany wynik i gdzie go zweryfikować. Scenariusze
**mutują trwały stan** — jeśli jeden scenariusz zablokuje/zmieni encję z seeda, kolejny scenariusz
odwołujący się do tej samej encji w tym samym stanie już nie zadziała tak samo (zaznaczone przy
każdym scenariuszu, który korzysta ze wspólnej encji).
