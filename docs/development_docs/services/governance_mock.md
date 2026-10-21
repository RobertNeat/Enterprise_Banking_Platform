# governance_mock — stan bieżący

Serwis Spring Boot 4.1.1 (Java 25), wewnętrzny (bez portu na hoście), wołany wyłącznie przez `banking_backend`. Port wewnętrzny: `10524` (`server.port` w `application.properties`, nadpisywalny przez env `SERVER_PORT` — zmienna środowiskowa ma priorytet nad plikiem properties, więc `deploy/compose.yml` działa bez zmian w kodzie).

Stan trwały w dedykowanej instancji PostgreSQL (kontener `mocks-postgres` w `deploy/compose.yml`/`compose.dev.yml`, osobnej od instancji `banking_backend`), schema `governance` — Spring Data JPA + Liquibase (ten sam mechanizm co `banking_backend`, zob. `db/changelog/`). Każdy przyszły zmigrowany mock (`identity_access_mock`, `external_institutions_gateway_mock`) dostanie własny schema w tej samej instancji. Zależności pom.xml: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-liquibase`, `spring-boot-starter-test`.

## Panel operatorski (`/`)

Serwis ma minimalną stronę placeholder pod `GET /` (Thymeleaf, `ui/UiController`), z współdzielonym stylem (`/css/theme.css` + `/css/theme-vars.css`, zmienna CSS `--primary-color` do odróżnienia kolorystyki per-serwis) — ten sam wzorzec fragmentów co `identity_access_mock`/`external_institutions_gateway_mock`. Strona informuje, że pełny panel operatorski (jak w `identity_access_mock`/`external_institutions_gateway_mock`) jeszcze nie istnieje, i wskazuje dostępne REST API.

## Zaimplementowane endpointy

Pełny kontrakt JSON: `docs/governance_mock_contract/api.md`.

- **Approval queue (maker-checker)**: `POST /approvals`, `GET /approvals` (filtr `status`), `GET /approvals/{id}`, `POST /approvals/{id}/decide`. Decyzja wymaga `decidedBy != requestedBy` (409 w przeciwnym razie) i statusu `PENDING` (409 jeśli już zdecydowany). Każda decyzja automatycznie dopisuje wpis do audit log.
- **Audit log**: `POST /audit-events`, `GET /audit-events` (filtry `entityType`, `entityId`).
- **ICT incident registry (DORA)**: `POST /ict-incidents`, `GET /ict-incidents` (filtry `status`, `severity`), `POST /ict-incidents/{id}/resolve` (409 jeśli już `RESOLVED`).
- **F8 — SLA/eskalacja spraw klientów**: `POST /cases/{caseId}/sla-check` — eskalacja automatyczna (`escalated: true` + wpis audytowy `AUTO_ESCALATED`) gdy `slaRemainingHours <= 0`.
- **Health**: `GET /actuator/health` (Spring Boot Actuator, bez dodatkowego endpointu `/health`).

Błędy walidacyjne/konfliktowe (400/404/409) zwracane jako `ProblemDetail` (RFC 9457) — włączone przez `spring.mvc.problemdetails.enabled=true`, obejmuje zarówno ręcznie rzucane `ResponseStatusException`, jak i błędy deserializacji JSON (np. nieznana wartość enum) obsługiwane przez domyślny mechanizm Spring MVC.

### Seedowanie startowe

Zmienna `GOVERNANCE_SEED_MOCK_DATA` (domyślnie `true`, patrz `.env.example`/`deploy/compose.yml`, property `governance.seed-mock-data`) jest mapowana przez `config/MockDataContextConfig` na Liquibase context `mock-data` (ten sam wzorzec co `banking_backend`'s `MockDataContextConfig`). Changeset `db/changelog/seed/001-approval.sql` (context `mock-data`) zasiewa wpisy zgodne z seedem `loan_application`/`loan_stage_transition` w `banking_backend` (`db/changelog/seed/002-loan.sql`):

- `11111111-1111-4111-8111-111111111001` — `LOAN_STAGE_TRANSITION`, `LN-80231` (jedyny wniosek, który w seedzie backendu faktycznie przeszedł `decision → contract`), status `APPROVED`, `requestedBy=PRC-10234`, `decidedBy=PRC-00001`.
- `22222222-2222-4222-8222-222222222002` — `LOAN_STAGE_TRANSITION`, `LN-80255` (nadal w `stage='decision'`, brak wiersza w `loan_stage_transition` — nikt jeszcze nie zdecydował), status `PENDING`, `requestedBy=PRC-20456`.
- `44444444-4444-4444-8444-444444444001` — `ENFORCEMENT_TRANSFER`, zajęcie `EN-2201` (`active` w seedzie backendu), status `APPROVED`, `requestedBy=PRC-30678`, `decidedBy=PRC-00001`. Odpowiadające wpisy audytowe (`APPROVAL_APPROVED`) dla obu zdecydowanych wniosków są zasiewane w tym samym changesecie.

Ustawienie `GOVERNANCE_SEED_MOCK_DATA=false` mapuje się na nieistniejący context Liquibase (`schema-only`), więc changeset seeda nigdy nie uruchamia się — serwis startuje z pustą kolejką zatwierdzeń/audytem. Ponieważ seed jest teraz śledzony przez Liquibase (a nie zasiewany przy każdym starcie z kodu), zmiana tej zmiennej po pierwszym starcie NIE usuwa/dodaje wierszy retroaktywnie — żeby zresetować dane, trzeba usunąć wolumen `mocks-postgres-data` (zob. sekcja "Reset danych" poniżej).

## Trwałość danych (Spring Data JPA + Liquibase)

Trzy encje JPA (`ApprovalRequestRecord`, `AuditEvent`, `IctIncident`) w schema `governance`, każda z odpowiadającym `JpaRepository` (`ApprovalRepository`, `AuditEventRepository`, `IctIncidentRepository`). Encje zachowują record-style akcesory (`id()`, `status()`, ...) obok JavaBean getterów — serwisy/kontrolery/testy wywołujące starą składnię kompilują się bez zmian, a Jackson serializuje przez gettery (`getId()`, `getStatus()`, ...), więc JSON-owy kontrakt REST jest identyczny jak przed migracją.

Filtrowanie (`AuditService.list(entityType, entityId)`, `IctIncidentService.list(status, severity)`) jest rozwiązane jawnymi metodami zapytań Spring Data per kombinacja filtrów (nie JPQL z `(:x IS NULL OR ...)`), żeby uniknąć problemów z nietypowanymi bindowaniami null na Postgres. `entityType` dopasowywany bez uwzględnienia wielkości liter (`findByEntityTypeIgnoreCase...`), `entityId` z uwzględnieniem — tak jak w poprzedniej wersji w pamięci.

Atomowość `ApprovalService.decide()`/`IctIncidentService.resolve()` (check-then-set, wcześniej zapewniana przez `ConcurrentHashMap.compute()`) jest teraz realizowana przez `@Transactional` + blokadę `PESSIMISTIC_WRITE` (`findByIdForUpdate` w obu repozytoriach), żeby dwa równoległe wywołania nie mogły obie przejść walidację stanu i obie zapisać.

Liquibase (`db/changelog/db.changelog-master.yaml`) — schema najpierw (`schema/001-approval.sql`, `002-audit.sql`, `003-incident.sql`, tabele jawnie kwalifikowane `governance.*`, bo Liquibase nie przepisuje raw-SQL changesetów pod `default-schema`), potem seed (`seed/001-approval.sql`, context `mock-data`). Schema `governance` tworzona idempotentnie przez `spring.datasource.hikari.connection-init-sql=CREATE SCHEMA IF NOT EXISTS governance` (działa identycznie na H2 w testach i na Postgres w produkcji/dev — ważne, bo `deploy_compose_ssh.sh` kopiuje na produkcję tylko `compose.yml`, więc skrypt inicjalizujący montowany w `docker-entrypoint-initdb.d` nie wchodziłby w życie). `spring.jpa.hibernate.ddl-auto=validate` — Liquibase jest jedynym właścicielem schematu (ta sama konwencja co `banking_backend`).

## Struktura kodu

```
src/main/java/robert_neat/governance_mock/
  audit/                          — AuditEvent (@Entity), AuditEventRepository, AuditService, AuditController
  approval/                       — ApprovalRequestRecord (@Entity), ApprovalRepository, ApprovalService, ApprovalController
  incident/                       — IctIncident (@Entity), IctIncidentRepository, IctIncidentService, IctIncidentController
  casesla/                        — CaseSlaController (F8)
  config/                         — MockDataContextConfig (GOVERNANCE_SEED_MOCK_DATA -> Liquibase context "mock-data")
src/main/resources/db/changelog/  — db.changelog-master.yaml, schema/, seed/
```

## Testy

`src/test/java/.../GovernanceMockApplicationTests.java` — `@SpringBootTest` (domyślny `webEnvironment=MOCK`, bez otwierania portu) + ręcznie budowany `MockMvc`, zamiast `@WebMvcTest`/`TestRestTemplate`/`@AutoConfigureMockMvc`. Testy pokrywają: odrzucenie samo-zatwierdzenia (409), poprawną decyzję + pojedynczy wpis audytowy, odrzucenie powtórnej decyzji (409), 404 dla nieznanego approval, eskalację SLA (<=0) z wpisem audytowym, brak eskalacji (>0) bez wpisu, 400 dla brakującego pola, cykl incydentu ICT (create → resolve → powtórne resolve = 409), 400 dla niekompletnego żądania approval, 400 z `ProblemDetail` dla nieprawidłowej wartości enum.

Mechanizm testowy: H2 w pamięci (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`), z Liquibase odtwarzającym cały schemat (`spring.jpa.hibernate.ddl-auto=validate`), ta sama konwencja co testy `banking_backend`. Każdy `@SpringBootTest` dostaje własny, izolowany H2 URL (`spring.datasource.url` w `@TestPropertySource`), bo jedna pamięciowa baza żyje przez cały czas działania Surefire (`DB_CLOSE_DELAY=-1`) i współdzielone dane między kontekstami popsułyby asercje (`ApprovalSeedingTest`/`ApprovalSeedingDisabledTest`/`GovernanceMockApplicationTests`/`IctIncidentServiceTest` mają każdy swój URL). `IctIncidentServiceTest` został przepisany z ręcznego `new IctIncidentService()` na `@SpringBootTest` z wstrzykiwanym serwisem/repozytorium i czyszczeniem tabeli w `@BeforeEach` (`incidentRepository.deleteAll()`), bo Spring Data JPA wymaga kontekstu Springa/EntityManagera.

## Uruchomienie lokalne poza Dockerem

Wymaga działającej instancji `mocks-postgres` (np. wystawionej na `127.0.0.1:10527` przez `compose.dev.yml`) — domyślne dane logowania w `application.properties` (`mocks`/`mocks`) nie muszą zgadzać się z hasłem z `.env`; w takim przypadku nadpisać `SPRING_DATASOURCE_PASSWORD` (ta sama konwencja co `banking_backend`).

## Reset danych

Stan żyje teraz w wolumenie Dockera `mocks-postgres-data` (kontener `mocks-postgres`), nie w pamięci procesu — restart kontenera `governance-mock` NIE czyści danych. Żeby zresetować wyłącznie dane tego mocka: zatrzymać stos i usunąć tylko wolumen `mocks-postgres-data` (np. `docker volume rm enterprise-banking-platform_mocks-postgres-data` po `docker compose down`, bez `-v`). NIE używać `docker compose down -v` — to usuwa też wolumen `postgres-data` (dane `banking_backend`).

## Do uzupełnienia w innych częściach aplikacji

- `banking_backend` musi przekazywać stabilne `entityId` (np. id sprawy/wniosku/zajęcia) przy wywołaniach `POST /audit-events` i `POST /cases/{caseId}/sla-check`, aby wpisy audytowe i eskalacje można było korelować z encjami domenowymi.
- `POST /cases/{caseId}/sla-check` nie ma deduplikacji — wielokrotne wywołanie dla tej samej sprawy przy przekroczonym SLA tworzy kolejne wpisy `AUTO_ESCALATED`. `banking_backend` powinien wywoływać ten endpoint raz per przekroczenie progu (np. przy przejściu z `inProgress` do `escalated`), a nie przy każdym odświeżeniu widoku.
- Rozwiązanie zatwierdzeń (`decide`) porównuje `decidedBy` i `requestedBy` jako zwykłe stringi (`trim` + ignorowanie wielkości liter) — rozwiązane w R1.5: `banking_backend`'s `/approvals/{id}/decide` proxy ustawia `decidedBy` z `sub` JWT (zawsze `PRC-xxxxx`), nigdy z ciała żądania klienta.
- Brak autoryzacji/uwierzytelnienia na poziomie tego serwisu — zakładane jest, że sieć wewnętrzna (`banking-internal`) i `banking_backend` są jedynym wołającym; `identity_access_mock` odpowiada za RBAC na poziomie UI/`banking_backend`.
