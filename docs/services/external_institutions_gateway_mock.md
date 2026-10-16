# external_institutions_gateway_mock — stan bieżący

Serwis Spring Boot 4.1.1 (Java 25), **eksponowany na hoście** (wołany bezpośrednio z przeglądarki / `banking_web`, nie tylko z `banking_backend`). Port: `10523` (`server.port` w `application.properties`, nadpisywalny przez env `SERVER_PORT`, patrz `deploy/compose.yml`).

Konsoliduje trzy integracje z instytucjami zewnętrznymi: bramę e-zajęć komorniczych (e-bailiff gateway), generator FATCA/CRS XML, bramę sprawozdawczą KNF/NBP/BFG.

Stan przechowywany wyłącznie w pamięci procesu (`ConcurrentHashMap` w komponentach `@Service`) — brak bazy danych, brak Spring Data JPA w tej iteracji (planowana w drugiej sesji, patrz niżej). Zależności pom.xml: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-thymeleaf` (UI), `spring-boot-starter-test`. FATCA/CRS XML generowany ręcznie (String-builder) — bez JAXB.

## Zaimplementowane endpointy

Pełny kontrakt JSON: `docs/external_institutions_gateway_mock_contract/api.md`.

- **E-zajęcia (F6)**: `POST /enforcement/incoming` (rejestracja, status `registered`), `GET /enforcement` (skrzynka odbiorcza), `POST /enforcement/{id}/transfer` (rozliczenie, status `settled`, 409 jeśli już rozliczone, 404 jeśli nieznane).
- **FATCA/CRS export**: `POST /fatca-crs/export` — generuje XML (korzeń `<FatcaCrsReport>`, `<Period>`, `<Records><Record>`), zapisuje w pamięci do podglądu w UI.
- **Brama sprawozdawcza KNF/NBP/BFG (F7)**: `POST /regulatory-gateway/submit` — natychmiastowy `202 PENDING`, po `gateway.submission.delay-ms` (domyślnie 2500 ms) status zmienia się w tle na `ACCEPTED`/`REJECTED` (reguła deterministyczna: `hash(reportId) mod 10 == 0` → `REJECTED`). `GET /regulatory-gateway/submissions/{id}` — polling statusu.
- **CORS**: endpointy REST mają włączony CORS (`gateway.cors.allowed-origin-patterns`, domyślnie `localhost`/`127.0.0.1` na każdym porcie) — wymagane, bo serwis jest wołany z innego originu przez przeglądarkę.
- **Health**: `GET /actuator/health`.

### Seedowanie startowe

Zmienna `GATEWAY_SEED_MOCK_DATA` (domyślnie `true`, patrz `.env.example`/`deploy/compose.yml`, property `gateway.seed-mock-data`) kontroluje, czy `EnforcementService` zasiewa przy starcie (`@PostConstruct`) wpisy zgodne z `enforcement_case` w `banking_backend` (`db/changelog/seed/009-enforcement.sql`): `KM 1123/26` (CUST-006, `active`), `KM 998/26` (CUST-004, `settled`), `KM 887/25` (CUST-005, `closed`), `KM 1145/26` (CUST-002, `registered`). Identyfikatory gatewaya są fikcyjne UUID (nie te z `enforcement_case.id` w backendzie) — backendowa kolumna `gateway_enforcement_id` jest w seedzie `NULL`, powiązanie id-do-id nie jest jeszcze zrobione (patrz "Do uzupełnienia" niżej). Ustawienie `GATEWAY_SEED_MOCK_DATA=false` startuje z pustym rejestrem zajęć (jak przed R1.5).

## UI operatorski (Thymeleaf)

Natywne fragmenty Thymeleaf (`th:replace`), bez Layout Dialect — wspólny layout w `fragments/layout.html` (head/header/footer w jednym pliku, wzorzec "Basic inclusion" z thymeleaf.org). Stylowanie przez `static/css/theme.css` (bazowe style panelu operatorskiego) i `static/css/theme-vars.css` (zmienna `--primary-color` tego serwisu). Nawigacja w nagłówku korzysta z `th:href`.

- `GET /` — realna strona startowa (`index.html`) z opisem i linkami do trzech sekcji (nie przekierowanie).
- `GET /ui/enforcement` + formularz `POST /ui/enforcement` — ręczna symulacja przychodzącego zajęcia komorniczego.
- `GET /ui/reports` — lista zgłoszeń do bramy sprawozdawczej ze statusami.
- `GET /ui/fatca` — lista wygenerowanych eksportów FATCA/CRS z podglądem XML (`<pre th:text>`, bez `th:utext` — unikanie XSS).

Kontrolery UI (`UiController`, `@Controller`) są osobne od REST API i czytają z tych samych serwisów w pamięci. Formularz HTML wysyła dane jako `x-www-form-urlencoded`, więc nie reużywa kontrolerów REST (`@RequestBody` JSON) — ma własny handler `@RequestParam`. `UiFooterAdvice` (`@ControllerAdvice`) wstrzykuje `serverPort` do modelu widoków UI, używane w stopce layoutu.

## Struktura kodu

```
src/main/java/robert_neat/external_institutions_gateway_mock/
  enforcement/   — EnforcementCase, EnforcementStatus, EnforcementService, EnforcementController
  fatca/         — FatcaCrsRecord, FatcaCrsExport(Request), FatcaCrsService, FatcaCrsController
  regulatory/    — RegulatorySubmission, SubmissionStatus, RegulatoryGatewayService, RegulatoryGatewayController
  config/        — CorsConfig
  ui/            — UiController, UiFooterAdvice
```

## Testy

`ExternalInstitutionsGatewayMockApplicationTests.java` — `@SpringBootTest` z manualnym `MockMvc` (jak w `governance_mock`), `gateway.submission.delay-ms=50` w test properties dla szybkości. Pokrywa: cykl zajęcia (incoming → list → transfer → powtórny transfer = 409), 404 dla nieznanego zajęcia, 400 dla niekompletnego żądania, eksport FATCA/CRS z escapowaniem `&`, 400 dla błędnego `recipient`, rozstrzygnięcie zgłoszenia przez polling (ACCEPTED/REJECTED), deterministyczny przypadek REJECTED, 400 dla nieznanego `recipient` bramy, 404 dla nieznanego `submissionId`, render wszystkich stron `/ui/**`.

`UiControllerTest.java` — weryfikuje render stron UI przez layout (`class="app-header"`/`class="app-footer"`), obecność linków do `/css/theme.css`/`/css/theme-vars.css`, serwowanie obu plików CSS pod `/css/**` (z zawartością `.card`/`--primary-color`) oraz przekierowanie formularza zajęć bez przecieku atrybutów modelu do URL.

## Do uzupełnienia w innych częściach aplikacji

- `banking_backend` musi odpytywać `GET /enforcement` okresowo (polling), aby wykrywać nowe przychodzące zajęcia komornicze — serwis nie wysyła żadnych webhooków/powiadomień push.
- Docelowy XSD dla eksportu FATCA/CRS nie został ustalony — obecny XML jest uproszczonym mockiem strukturalnym (korzeń `FatcaCrsReport`), nie odwzorowuje żadnego konkretnego standardu IRS/OECD. Do uzgodnienia przed integracją produkcyjną.
- Prawdziwa integracja z systemem Ognivo (e-zajęcia komornicze) jest poza zakresem — obecny endpoint `/enforcement/incoming` tylko symuluje odebranie tytułu wykonawczego.
- Reguła deterministyczna `REJECTED`/`ACCEPTED` bramy sprawozdawczej (`hash(reportId) mod 10 == 0`) jest mockiem — prawdziwa walidacja KNF/NBP/BFG nie jest tu implementowana.
- Następna sesja (druga iteracja): zamiana `ConcurrentHashMap` na trwały storage (Spring Data JPA + baza danych) z seederem — kontrakt REST pozostaje bez zmian.
- Brak autoryzacji/uwierzytelnienia — serwis jest mockiem eksponowanym na hoście; w środowisku produkcyjnym wymagałby zabezpieczenia (np. mTLS/API key) ze względu na ekspozycję poza siecią wewnętrzną.
