# identity_access_mock

## Stan bieżący

Spring Boot 4.1.1, Java 25, port domyślny `10525` (env `SERVER_PORT`), eksponowany na hoście — wołany
bezpośrednio z przeglądarki (banking_web). Stan trwały w dedykowanej instancji PostgreSQL (kontener
`mocks-postgres` w `deploy/compose.yml`/`compose.dev.yml`, wspólnej z `governance_mock`, osobnej od
instancji `banking_backend`), schema `identity_access` — Spring Data JPA + Liquibase (ten sam
mechanizm co `banking_backend`/`governance_mock`, zob. `db/changelog/`). Brak Spring Security i Spring
Authorization Server — ewentualne przejście z własnego tokenu UUID na te mechanizmy zostaje poza
zakresem tej iteracji. Zależności pom.xml: `spring-boot-starter-web`, `spring-boot-starter-actuator`,
`spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `postgresql`,
`spring-boot-starter-liquibase`, `spring-boot-starter-test`.

Konsoliduje trzy mockowane funkcje: RBAC (rola → uprawnienia), OTP/MFA (PSD2 SCA), rejestr zaufanych
urządzeń mobilnych. Realizuje F1 (logowanie + uprawnienia) i F5 (odblokowanie dostępu e-banking z silnym
uwierzytelnieniem) z `docs/banking_web_functions(concise).md`.

### Moduły

- **auth** — konta pracowników (`EmployeeAccount`), logowanie/wylogowanie, rejestracja, statyczna mapa
  rola→uprawnienia (`RolePermissions`). 6 kont demo zgodnych z `DemoAccount` z banking_web
  (`PRC-00001`…`PRC-50123`). Token sesji to zwykły UUID (nie JWT), przekazywany w `Authorization: Bearer <token>`
  i trzymany w tabeli `auth_session` (token → employeeId, zastępuje starą mapę `sessionsByToken`).
  `RolePermissions` jest świadomie bez hierarchii ("no persistence, no hierarchy") — permissions są
  wyliczane z `role` (`EmployeeAccount.getPermissions()`, metoda `@Transient`), nigdy nie są
  przechowywane w bazie. Hierarchia (`seniorityLevel`) żyje wyłącznie w `banking_backend`'s
  `auth/RoleCode.java` (R1.5). Permission `"approvals"` (dodana w R1.5, tylko dla `ADMIN`/`COMPLIANCE`)
  gatuje widok kolejki zatwierdzeń (maker-checker) w `banking_web` — to osobny mechanizm od
  hierarchii, nie łamie konwencji "no hierarchy" (permissions i seniority to różne osie).
- **otp** — generowanie i weryfikacja 6-cyfrowych kodów OTP (TTL 5 minut, jednorazowe — po pozytywnej
  weryfikacji kod jest oznaczany jako zużyty). Kod nigdy nie wraca w odpowiedzi REST — jest widoczny
  tylko w panelu operatorskim (`/otp`). `verify()` i `consumeVerified()` blokują wiersz
  (`PESSIMISTIC_WRITE`, `OtpCodeRepository.findByIdForUpdate`) na czas transakcji, żeby równoległe
  wywołania nie mogły obie przejść walidacji i obie zapisać stan (replay OTP).
- **device** — rejestr urządzeń mobilnych (`deviceId`, właściciel = employeeId lub customerId/userId
  e-bankingu, model, `lastSeenAt`, `trusted`). Reset urządzenia wymaga wcześniej zweryfikowanego OTP
  o celu `RESET_DEVICE`. `unbind()`/`reset()` też blokują wiersz urządzenia (`PESSIMISTIC_WRITE`)
  na czas transakcji.
- **ebanking** — `POST /ebanking-access/{userId}/unlock` potwierdza odblokowanie dostępu po weryfikacji
  OTP o celu `UNLOCK_ACCESS`. Sam byt `EbankingUser` (status `accessStatus`) żyje w `banking_backend` —
  ten serwis tylko certyfikuje, że silne uwierzytelnienie się powiodło.

### Panel operatorski (Thymeleaf)

Strony `@Controller` (nie REST) pod `/`, `/accounts`, `/otp`, `/devices`, `/ebanking` — czytają z tych
samych serwisów co REST API. Wspólny layout (`templates/fragments/layout.html`) z natywnymi
fragmentami Thymeleaf (`th:replace`, parametryzowany fragment `head(title)`), bez Layout Dialect.
Stylowanie: `static/css/theme-vars.css` (zmienna `--primary-color`, osobna per-serwis, do ręcznego
dostrojenia) + `static/css/theme.css` (wspólny bazowy wygląd — nagłówek, tabele, formularze, karty
`.card`) wspólne dla wszystkich mocków platformy. Panel `/otp` to jedyne miejsce, gdzie widoczny jest
wygenerowany kod OTP — służy do manualnego testowania integracji. Panel `/devices` ma formularz "Unbind"
(POST/redirect/GET na `/devices/{deviceId}/unbind-ui`, osobno od REST-owego `/devices/{deviceId}/unbind`).
Panel `/ebanking` udostępnia formularz odblokowania dostępu (userId, otpId, kod OTP) — wywołuje
weryfikację i konsumpcję OTP w jednym kroku (POST/redirect/GET na `/ebanking/unlock-ui`, osobno od
REST-owego `POST /ebanking-access/{userId}/unlock`, który wymaga już zweryfikowanego OTP). Panel nie
pokrywa jeszcze `/auth/**`, `/otp/request`, `/otp/verify` ani `/devices/{deviceId}/reset` — te funkcje są
dostępne tylko przez REST.

### CORS

Serwis jest wołany bezpośrednio z przeglądarki na innym porcie (banking_web, domyślnie 10500), więc
endpointy REST (`/auth/**`, `/otp/**`, `/devices/**`, `/ebanking-access/**`) mają skonfigurowany CORS
(właściwość `app.cors.allowed-origin`, domyślnie `http://localhost:10500`; nadpisywalna przez zmienną
środowiskową `BANKING_WEB_ORIGIN`, przekazywaną w `deploy/compose.yml`). Na produkcji/LAN musi być
ustawiona na rzeczywisty origin `banking_web` (np. `http://192.168.1.160:10500`), inaczej CORS
zablokuje wywołania z przeglądarki — wartość ustawiana w `config.env` na hoście.

### Seedowanie startowe

Zmienna `IDENTITY_ACCESS_SEED_MOCK_DATA` (domyślnie `true`, patrz `.env.example`/`deploy/compose.yml`,
property `identity-access.seed-mock-data`) jest mapowana przez `config/MockDataContextConfig` na
Liquibase context `mock-data` (ten sam wzorzec co `governance_mock`). Changesety
`db/changelog/seed/001-employee-account.sql` (6 kont demo, wcześniej zasiewane w konstruktorze
`AuthService`) i `seed/002-device.sql` (3 urządzenia demo, wcześniej zasiewane w konstruktorze
`DeviceService`) są uruchamiane tylko pod tym contextem. Ustawienie `IDENTITY_ACCESS_SEED_MOCK_DATA=false`
mapuje się na nieistniejący context (`schema-only`), więc serwis startuje z pustymi tabelami kont/
urządzeń — `banking_backend` nie będzie mieć żadnego konta do zalogowania.

## Trwałość danych (Spring Data JPA + Liquibase)

Cztery encje JPA w schema `identity_access`: `EmployeeAccount`, `AuthSession`, `Device`, `OtpCode`,
każda z odpowiadającym `JpaRepository`. `AuthSession` zastępuje starą mapę `sessionsByToken: Map<String,
String>` — `token` jest kluczem głównym (PK), `employeeId` zwykłą kolumną z `REFERENCES
identity_access.employee_account (employee_id)` w schemacie SQL (nie `@ManyToOne` — `open-in-view`
jest `false` i nic nie leniwie doczytuje konta z sesji). `EmployeeAccount.getPermissions()` jest
`@Transient` — uprawnienia są wyliczane z `role` przez `RolePermissions.forRole()`, nigdy
przechowywane (ten sam kontrakt "no persistence" co przed migracją).

Atomowość operacji jednorazowych/zmiennych stanowo (wcześniej zapewniana przez `ConcurrentHashMap`):
`OtpService.verify()`/`consumeVerified()` i `DeviceService.unbind()`/`reset()` blokują wiersz
(`@Transactional` + `PESSIMISTIC_WRITE`, `findByIdForUpdate` w `OtpCodeRepository`/
`DeviceRepository`) na czas transakcji, żeby dwa równoległe wywołania nie mogły obie przejść
walidacji stanu i obie zapisać (replay OTP, race na `trusted`/`lastSeenAt`).

Liquibase (`db/changelog/db.changelog-master.yaml`) — schema najpierw (`schema/001-employee-account.sql`
… `004-otp-code.sql`, tabele jawnie kwalifikowane `identity_access.*`, bo Liquibase nie przepisuje
raw-SQL changesetów pod `default-schema`), potem seed (`seed/001-employee-account.sql`,
`seed/002-device.sql`, context `mock-data`). Schema `identity_access` tworzona idempotentnie przez
`spring.datasource.hikari.connection-init-sql=CREATE SCHEMA IF NOT EXISTS identity_access` (działa
identycznie na H2 w testach i na Postgres w produkcji/dev). `spring.jpa.hibernate.ddl-auto=validate`
— Liquibase jest jedynym właścicielem schematu.

## Struktura kodu

```
src/main/java/robert_neat/identity_access_mock/
  auth/     — EmployeeAccount (@Entity), AuthSession (@Entity), EmployeeAccountRepository,
              AuthSessionRepository, AuthService, AuthController, RolePermissions
  otp/      — OtpCode (@Entity), OtpCodeRepository, OtpService, OtpController
  device/   — Device (@Entity), DeviceRepository, DeviceService, DeviceController
  ebanking/ — EbankingAccessController
  config/   — MockDataContextConfig (IDENTITY_ACCESS_SEED_MOCK_DATA -> Liquibase context "mock-data")
src/main/resources/db/changelog/ — db.changelog-master.yaml, schema/, seed/
```

## Testy

Wszystkie testy kontrolerów (`AuthControllerTest`, `DeviceControllerTest`, `OtpControllerTest`,
`EbankingAccessControllerTest`) zostały przepisane z ręcznego MockMvc standalone (`new AuthService()`
itd.) na `@SpringBootTest` + pełny `WebApplicationContext`, bo Spring Data JPA wymaga kontekstu
Springa/EntityManagera. Mechanizm testowy: H2 w pamięci (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`),
Liquibase odtwarza cały schemat łącznie z seedem (`spring.jpa.hibernate.ddl-auto=validate`). Każdy
`@SpringBootTest` dostaje własny, izolowany H2 URL (`spring.datasource.url` w `@TestPropertySource`),
bo jedna pamięciowa baza żyje przez cały czas działania Surefire (`DB_CLOSE_DELAY=-1`).

Testy, które zależą od danych zasiewanych przez Liquibase (konta w `AuthControllerTest`, urządzenia w
`DeviceControllerTest`/`UiControllerTest`), NIE czyszczą swoje tabele w `@BeforeEach` — czyszczą tylko
tabele bez seeda (`otp_code` w `DeviceControllerTest`/`OtpControllerTest`/`EbankingAccessControllerTest`).
`AuthControllerTest` usuwa w `@AfterEach` konto `PRC-99999` utworzone przez test rejestracji (oraz
jego sesję w `auth_session`, jeśli test zatwierdzenia+logowania ją utworzył — FK na `employee_id`
wymaga usunięcia sesji przed kontem), żeby asercja liczby kont
(`accountsListsSeededDemoAccountsWithoutPasswords`, oczekuje 6) była niezależna od kolejności
wykonania testów w tej samej klasie.

## Reset danych

Stan żyje w wolumenie Dockera `mocks-postgres-data` (kontener `mocks-postgres`, współdzielony z
`governance_mock`/`external_institutions_gateway_mock`) — restart kontenera `identity-access-mock`
NIE czyści danych. Żeby zresetować wyłącznie dane tego mocka bez wpływu na schema `governance`/
`gateway`, trzeba by usunąć tylko tabele schematu `identity_access` (np. `DROP SCHEMA identity_access
CASCADE;` w `psql`) — usunięcie całego wolumenu `mocks-postgres-data` resetuje WSZYSTKIE TRZY
schematy (`governance`, `identity_access`, `gateway`). NIE używać `docker compose down -v` — to
usuwa też wolumen `postgres-data` (dane `banking_backend`).

## Zatwierdzanie zarejestrowanych kont

Rejestracja (`POST /auth/register`) zostawia konto w stanie `PENDING_APPROVAL` z `role=null`.
Administrator (uprawnienie `accounts-admin`, patrz `RolePermissions.ADMIN`) zatwierdza lub odrzuca
konto w `banking_web` pod `/accounts` — `POST /auth/accounts/{id}/approve` (przypisuje rolę,
`status=ACTIVE`) albo `POST /auth/accounts/{id}/reject` (`status=REJECTED`, rekord zostaje,
`employeeId` pozostaje zajęte). Zatwierdzanie jest bezpośrednie — NIE przechodzi przez kolejkę
maker-checker `governance_mock`'s `/approvals` (ten mechanizm obsługuje inny proces biznesowy:
wnioski transakcyjne/kredytowe pracowników, nie cykl życia konta pracownika).

## Do uzupełnienia w innych częściach aplikacji

- **banking_backend / EbankingUser**: po pozytywnym `POST /ebanking-access/{userId}/unlock` backend
  powinien przestawić `accessStatus` z `locked`/`pendingReset` na `active` we własnej encji.
