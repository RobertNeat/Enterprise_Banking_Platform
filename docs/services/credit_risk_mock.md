# credit_risk_mock — stan bieżący

Serwis wewnętrzny (Spring Boot 4.1.1, Java 25), bez portu na hoście — wołany wyłącznie przez `banking_backend`. Port wewnętrzny: `10522` (env `SERVER_PORT`, patrz `deploy/compose.yml`).

Konsoliduje dwa mocki: biuro informacji kredytowej (BIK / scoring zewnętrzny) oraz silnik kalkulacji rezerw ECL (IFRS 9).

Stan przechowywany wyłącznie w pamięci procesu (`ConcurrentHashMap` w singletonach `@Service`) — brak bazy danych, brak Spring Data JPA. Dane nie przetrwają restartu.

## Zaimplementowane endpointy

### Credit bureau (BIK mock)

- `POST /credit-bureau/score` — przyjmuje `{ applicantId, requestedAmount, segment }` (`segment`: `retail`|`business`), zwraca symulowany wynik scoringowy i DSTI. `scoreResult` jest pseudo-losowy, ale deterministyczny (hash z `applicantId`, zakres 300-850, skala FICO/BIK). `dsti` jest pseudo-losowy, skorelowany z `requestedAmount` (większa kwota → wyższe DSTI), zakres 0.00-0.99. Każde zapytanie zapisywane do historii per `applicantId`.
- `GET /credit-bureau/history/{applicantId}` — lista wcześniejszych zapytań dla wnioskodawcy (chronologicznie, może być pusta).

### ECL engine (IFRS 9)

- `POST /ecl/classify` — przyjmuje `{ positionId, daysPastDue, customerRating, exposureAmount }`. Klasyfikacja koszyka: `daysPastDue <= 30` → `stage1`, `31-90` → `stage2`, `>90` → `stage3`. Rezerwa = `exposureAmount` * (1% / 5% / 25%) odpowiednio dla stage1/2/3. Wynik zapisywany jako najnowsza klasyfikacja dla danej pozycji (nadpisuje poprzednią).
- `GET /ecl/positions/{positionId}` — ostatnia klasyfikacja danej pozycji; 404 jeśli pozycja nie była jeszcze klasyfikowana.

Pełny kontrakt request/response: `docs/credit_risk_mock_contract/api.md`.

## Panel operatorski

Pod `/` dostępna jest minimalna strona placeholder (Thymeleaf, `spring-boot-starter-thymeleaf`), bez pełnego panelu operatorskiego jak w `identity_access_mock`/`external_institutions_gateway_mock`. Współdzielony styl: `theme.css` + `theme-vars.css` (zmienna CSS `--primary-color` do rozróżnienia kolorystyki per serwis).

## Walidacja

Walidacja ręczna w warstwie serwisowej (bez `spring-boot-starter-validation` — nie jest zależnością projektu). Błędy walidacji → HTTP 400 z body `{ "message": "..." }`. Brak zasobu (np. nieznana pozycja ECL) → HTTP 404 z tym samym formatem.

## Do uzupełnienia w innych częściach aplikacji

- `banking_backend` musi wywołać `POST /credit-bureau/score` w kroku scoringu kreatora wniosku kredytowego (KRD-05) i zapisać `scoreResult`/`dsti` do `LoanApplication`.
- `banking_backend` musi wywołać `POST /ecl/classify` przy przeliczaniu koszyka IFRS 9 i rezerwy dla `CollateralEntry` (RSK-01) oraz przy zmianie dni opóźnienia/ratingu pozycji.
- Potrzebny trwały storage (baza danych + seeder) w kolejnej iteracji — obecnie stan ginie po restarcie procesu.
- Rozważyć w kolejnej iteracji konfigurowalne progi DSTI/LTV per segment klienta (rekomendacje KNF T i S) — obecnie poza zakresem tego serwisu.
