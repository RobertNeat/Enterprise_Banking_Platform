# external_institutions_gateway_mock — stan bieżący

Serwis Spring Boot 4.1.1 (Java 25), **eksponowany na hoście** (wołany bezpośrednio z przeglądarki / `banking_web`, nie tylko z `banking_backend`). Port: `10523` (`server.port` w `application.properties`, nadpisywalny przez env `SERVER_PORT`, patrz `deploy/compose.yml`).

Konsoliduje trzy integracje z instytucjami zewnętrznymi: bramę e-zajęć komorniczych (e-bailiff gateway), generator FATCA/CRS XML, bramę sprawozdawczą KNF/NBP/BFG.

Stan trwały w dedykowanej instancji PostgreSQL (kontener `mocks-postgres` w `deploy/compose.yml`/`compose.dev.yml`, wspólnej z `governance_mock`/`identity_access_mock`, osobnej od instancji `banking_backend`), schema `gateway` — Spring Data JPA + Liquibase (ten sam mechanizm co `banking_backend`/`governance_mock`/`identity_access_mock`, zob. `db/changelog/`). Zależności pom.xml: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-thymeleaf` (UI), `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-liquibase`, `spring-boot-starter-test`. FATCA/CRS XML generowany ręcznie (String-builder) — bez JAXB.

## Zaimplementowane endpointy

Pełny kontrakt JSON: `docs/external_institutions_gateway_mock_contract/api.md`.

- **E-zajęcia (F6)**: `POST /enforcement/incoming` (rejestracja, status `registered`), `GET /enforcement` (skrzynka odbiorcza), `POST /enforcement/{id}/transfer` (rozliczenie, status `settled`, 409 jeśli już rozliczone, 404 jeśli nieznane).
- **FATCA/CRS export**: `POST /fatca-crs/export` — generuje XML (korzeń `<FatcaCrsReport>`, `<Period>`, `<Records><Record>`), zapisuje w pamięci do podglądu w UI.
- **Brama sprawozdawcza KNF/NBP/BFG (F7)**: `POST /regulatory-gateway/submit` — natychmiastowy `202 PENDING`, po `gateway.submission.delay-ms` (domyślnie 2500 ms) status zmienia się w tle na `ACCEPTED`/`REJECTED` (reguła deterministyczna: `hash(reportId) mod 10 == 0` → `REJECTED`). `GET /regulatory-gateway/submissions/{id}` — polling statusu. Rozstrzygnięcie w tle działa na osobnym wątku (`CompletableFuture.delayedExecutor`), poza transakcją żądania — `RegulatoryGatewayService` używa programatycznego `TransactionTemplate`, nie `@Transactional` (samo-wywołanie obeszłoby proxy AOP).
- **CORS**: endpointy REST mają włączony CORS (`gateway.cors.allowed-origin-patterns`, domyślnie `http://localhost:10500`, nadpisywalne przez zmienną `BANKING_WEB_ORIGIN` przekazywaną w `deploy/compose.yml`) — wymagane, bo serwis jest wołany z innego originu przez przeglądarkę; na produkcji/LAN musi wskazywać rzeczywisty origin `banking_web`.
- **Health**: `GET /actuator/health`.

### Seedowanie startowe

Zmienna `GATEWAY_SEED_MOCK_DATA` (domyślnie `true`, patrz `.env.example`/`deploy/compose.yml`, property `gateway.seed-mock-data`) jest mapowana przez `config/MockDataContextConfig` na Liquibase context `mock-data` (ten sam wzorzec co `governance_mock`/`identity_access_mock`). Changesety `db/changelog/seed/001-enforcement-case.sql` i `seed/002-regulatory-submission.sql` (wcześniej zasiewane w `@PostConstruct seed()` metodach `EnforcementService`/`RegulatoryGatewayService`) są uruchamiane tylko pod tym contextem:

- `enforcement_case`: `KM 1123/26` (CUST-006, `active`), `KM 998/26` (CUST-004, `settled`), `KM 887/25` (CUST-005, `closed`), `KM 1145/26` (CUST-002, `registered`). Identyfikatory gatewaya są fikcyjnymi, ale stałymi UUID (`33333333-3333-4333-8333-333333333001`..`004`) — te same wartości są wpisane w `enforcement_case.gateway_enforcement_id` w seedzie backendu (`db/changelog/seed/009-enforcement.sql`), więc po id-do-id są połączone już od startu bez potrzeby rejestrowania nowego zajęcia. `id` encji `EnforcementCase` jest tym samym polem — nie ma osobnej kolumny `gatewayEnforcementId`.
- `regulatory_submission`: `RPT-NBP-014` (NBP, seedowany `PENDING`) i `RPT-FATCA-009` (FATCA/CRS, `ACCEPTED`) — te same `submissionId` co w seedzie backendu (`db/changelog/seed/010-reporting.sql`). `RPT-NBP-014` zostaje rozstrzygnięty (deterministycznie, do `ACCEPTED`) przy starcie serwisu przez `RegulatoryGatewayService.reschedulePendingSubmissions()` (listener `ApplicationReadyEvent`), który planuje `resolve()` dla każdego wiersza wciąż `PENDING` — bez tego seedowany wiersz zostałby `PENDING` na zawsze (stary `@PostConstruct` sam planował to dla siebie, ale to podejście nie przenosi się na seed Liquibase).

Ustawienie `GATEWAY_SEED_MOCK_DATA=false` mapuje się na nieistniejący context (`schema-only`), więc serwis startuje z pustym rejestrem zajęć/zgłoszeń (jak przed R1.5).

## UI operatorski (Thymeleaf)

Natywne fragmenty Thymeleaf (`th:replace`), bez Layout Dialect — wspólny layout w `fragments/layout.html` (head/header/footer w jednym pliku, wzorzec "Basic inclusion" z thymeleaf.org). Stylowanie przez `static/css/theme.css` (bazowe style panelu operatorskiego) i `static/css/theme-vars.css` (zmienna `--primary-color` tego serwisu). Nawigacja w nagłówku korzysta z `th:href`.

- `GET /` — realna strona startowa (`index.html`) z opisem i linkami do trzech sekcji (nie przekierowanie).
- `GET /ui/enforcement` + formularz `POST /ui/enforcement` — ręczna symulacja przychodzącego zajęcia komorniczego.
- `GET /ui/reports` — lista zgłoszeń do bramy sprawozdawczej ze statusami.
- `GET /ui/fatca` — lista wygenerowanych eksportów FATCA/CRS z podglądem XML (`<pre th:text>`, bez `th:utext` — unikanie XSS).

Kontrolery UI (`UiController`, `@Controller`) są osobne od REST API i czytają z tych samych serwisów w pamięci. Formularz HTML wysyła dane jako `x-www-form-urlencoded`, więc nie reużywa kontrolerów REST (`@RequestBody` JSON) — ma własny handler `@RequestParam`. `UiFooterAdvice` (`@ControllerAdvice`) wstrzykuje `serverPort` do modelu widoków UI, używane w stopce layoutu.

## Trwałość danych (Spring Data JPA + Liquibase)

Trzy encje JPA w schema `gateway`: `EnforcementCase`, `FatcaCrsExport`, `RegulatorySubmission`, każda z odpowiadającym `JpaRepository`. Wszystkie trzy zachowują dual-accessor pattern z `governance_mock`'s `ApprovalRequestRecord` — akcesory w stylu rekordu (`id()`, `status()`, ...) obok JavaBean getterów — żeby istniejące wywołania (testy, szablony Thymeleaf) kompilowały się bez zmian; Jackson serializuje wyłącznie przez gettery.

- `EnforcementCase.id` jest jednocześnie identyfikatorem po stronie gatewaya referencjonowanym przez `banking_backend` (`enforcement_case.gateway_enforcement_id`) — nie ma osobnej kolumny `gatewayEnforcementId`. `transfer()` blokuje wiersz (`@Transactional` + `PESSIMISTIC_WRITE`, `EnforcementCaseRepository.findByIdForUpdate`) na czas transakcji, żeby dwa równoległe rozliczenia nie mogły obie przejść przez sprawdzenie statusu.
- `FatcaCrsExport.xml` jest kolumną `TEXT` (bez `@Lob`) — `@Lob` na encji JPA mapuje się na PostgreSQL na duży obiekt `oid`, nie na kolumnę `TEXT`, i złamałby `ddl-auto=validate` wobec zwykłego `TEXT`; ten sam wzorzec nieograniczonego tekstu co `governance.approval_request.payload`. `reportId` jest kluczem głównym — powtórny eksport z tym samym `reportId` nadpisuje poprzedni (ten sam kontrakt co stare `Map.put`).
- `RegulatorySubmission.validationErrors` (`List<String>`) jest `@ElementCollection` (fetch `EAGER`, bo `open-in-view` jest `false`) z własną tabelą `gateway.regulatory_submission_error` (`@OrderColumn` zachowuje kolejność) — najbardziej idiomatyczne odwzorowanie JPA dla listy stringów jako pola encji, bez serializacji do jednej kolumny tekstowej. `VALID_RECIPIENTS` zostaje w serwisie (walidacja żądania, nie trwały stan).

**Asynchroniczne rozstrzygnięcie zgłoszenia** (`RegulatoryGatewayService.resolve()`) działa na wątku `CompletableFuture.delayedExecutor`, poza transakcją żądania — samo-wywołanie z `@PostConstruct`/`submit()` obeszłoby proxy AOP Springa, więc `resolve()` używa programatycznego `TransactionTemplate` (bean zdefiniowany w `config/TransactionTemplateConfig`) zamiast `@Transactional`. `submit()` planuje `resolve()` dopiero po zatwierdzeniu transakcji zapisu (`TransactionSynchronizationManager.registerSynchronization`/`afterCommit`) — inaczej blokada wiersza w `resolve()` mogłaby zadziałać przed commitem insertu i nic nie znaleźć. `reschedulePendingSubmissions()` (listener `ApplicationReadyEvent`) planuje `resolve()` dla każdego wiersza zasiedzonego jako `PENDING` (dziś tylko `RPT-NBP-014`) — `resolve()` jest no-op, jeśli status nie jest już `PENDING`, więc podwójne zaplanowanie jest bezpieczne.

Liquibase (`db/changelog/db.changelog-master.yaml`) — schema najpierw (`schema/001-enforcement-case.sql`…`003-regulatory-submission.sql`, tabele jawnie kwalifikowane `gateway.*`, bo Liquibase nie przepisuje raw-SQL changesetów pod `default-schema`), potem seed (`seed/001-enforcement-case.sql`, `seed/002-regulatory-submission.sql`, context `mock-data`). Schema `gateway` tworzona idempotentnie przez `spring.datasource.hikari.connection-init-sql=CREATE SCHEMA IF NOT EXISTS gateway` (działa identycznie na H2 w testach i na Postgres w produkcji/dev). `spring.jpa.hibernate.ddl-auto=validate` — Liquibase jest jedynym właścicielem schematu.

## Struktura kodu

```
src/main/java/robert_neat/external_institutions_gateway_mock/
  enforcement/   — EnforcementCase (@Entity), EnforcementCaseRepository, EnforcementStatus,
                   EnforcementService, EnforcementController
  fatca/         — FatcaCrsRecord, FatcaCrsExport (@Entity), FatcaCrsExportRepository,
                   FatcaCrsExportRequest, FatcaCrsService, FatcaCrsController
  regulatory/    — RegulatorySubmission (@Entity), RegulatorySubmissionRepository, SubmissionStatus,
                   RegulatoryGatewayService, RegulatoryGatewayController
  config/        — CorsConfig, MockDataContextConfig (GATEWAY_SEED_MOCK_DATA -> Liquibase context
                   "mock-data"), TransactionTemplateConfig (programmatic tx bean for async resolve())
  ui/            — UiController, UiFooterAdvice
src/main/resources/db/changelog/ — db.changelog-master.yaml, schema/, seed/
```

## Testy

Wszystkie testy `@SpringBootTest` (`ExternalInstitutionsGatewayMockApplicationTests`, `EnforcementSeedingTest`, `EnforcementSeedingDisabledTest`, `RegulatorySubmissionSeedingTest`, `UiControllerTest`) dostają własny, izolowany H2 URL (`spring.datasource.url` w `@TestPropertySource`), bo jedna pamięciowa baza żyje przez cały czas działania Surefire (`DB_CLOSE_DELAY=-1`). H2 w trybie `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`, Liquibase odtwarza cały schemat łącznie z seedem (`spring.jpa.hibernate.ddl-auto=validate`). Żaden test nie czyści tabel w `@BeforeEach` — nic nie liczy wierszy ponad to, co sam stworzył, a tabele zasiedzone (`enforcement_case`, `regulatory_submission`) muszą zostać nienaruszone.

`ExternalInstitutionsGatewayMockApplicationTests.java` — `@SpringBootTest` z manualnym `MockMvc` (jak w `governance_mock`), `gateway.submission.delay-ms=50` w test properties dla szybkości. Pokrywa: cykl zajęcia (incoming → list → transfer → powtórny transfer = 409), 404 dla nieznanego zajęcia, 400 dla niekompletnego żądania, eksport FATCA/CRS z escapowaniem `&`, 400 dla błędnego `recipient`, rozstrzygnięcie zgłoszenia przez polling (ACCEPTED/REJECTED), deterministyczny przypadek REJECTED, 400 dla nieznanego `recipient` bramy, 404 dla nieznanego `submissionId`, render wszystkich stron `/ui/**`.

`EnforcementSeedingTest`/`EnforcementSeedingDisabledTest` i `RegulatorySubmissionSeedingTest` — weryfikują seed Liquibase (`GATEWAY_SEED_MOCK_DATA=true`/`false`) zamiast dawnego `@PostConstruct`; `RegulatorySubmissionSeedingTest` odpytuje (`pollUntilResolved`) `RPT-NBP-014` do rozstrzygnięcia, potwierdzając że `reschedulePendingSubmissions()` faktycznie podejmuje seedowany wiersz `PENDING`.

`UiControllerTest.java` — weryfikuje render stron UI przez layout (`class="app-header"`/`class="app-footer"`), obecność linków do `/css/theme.css`/`/css/theme-vars.css`, serwowanie obu plików CSS pod `/css/**` (z zawartością `.card`/`--primary-color`) oraz przekierowanie formularza zajęć bez przecieku atrybutów modelu do URL.

## Reset danych

Stan żyje w wolumenie Dockera `mocks-postgres-data` (kontener `mocks-postgres`, współdzielony z `governance_mock`/`identity_access_mock`) — restart kontenera `external-institutions-gateway-mock` NIE czyści danych. Żeby zresetować wyłącznie dane tego mocka, trzeba usunąć tylko tabele schematu `gateway` (np. `DROP SCHEMA gateway CASCADE;` w `psql`) — usunięcie całego wolumenu `mocks-postgres-data` resetuje WSZYSTKIE TRZY schematy (`governance`, `identity_access`, `gateway`). NIE używać `docker compose down -v` — to usuwa też wolumen `postgres-data` (dane `banking_backend`).

## Do uzupełnienia w innych częściach aplikacji

- `banking_backend` musi odpytywać `GET /enforcement` okresowo (polling), aby wykrywać nowe przychodzące zajęcia komornicze — serwis nie wysyła żadnych webhooków/powiadomień push.
- Docelowy XSD dla eksportu FATCA/CRS nie został ustalony — obecny XML jest uproszczonym mockiem strukturalnym (korzeń `FatcaCrsReport`), nie odwzorowuje żadnego konkretnego standardu IRS/OECD. Do uzgodnienia przed integracją produkcyjną.
- Prawdziwa integracja z systemem Ognivo (e-zajęcia komornicze) jest poza zakresem — obecny endpoint `/enforcement/incoming` tylko symuluje odebranie tytułu wykonawczego.
- Reguła deterministyczna `REJECTED`/`ACCEPTED` bramy sprawozdawczej (`hash(reportId) mod 10 == 0`) jest mockiem — prawdziwa walidacja KNF/NBP/BFG nie jest tu implementowana.
- Brak autoryzacji/uwierzytelnienia — serwis jest mockiem eksponowanym na hoście; w środowisku produkcyjnym wymagałby zabezpieczenia (np. mTLS/API key) ze względu na ekspozycję poza siecią wewnętrzną.
