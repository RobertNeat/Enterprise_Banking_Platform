# governance_mock — stan bieżący

Serwis Spring Boot 4.1.1 (Java 25), wewnętrzny (bez portu na hoście), wołany wyłącznie przez `banking_backend`. Port wewnętrzny: `10524` (`server.port` w `application.properties`, nadpisywalny przez env `SERVER_PORT` — zmienna środowiskowa ma priorytet nad plikiem properties, więc `deploy/compose.yml` działa bez zmian w kodzie).

Stan przechowywany wyłącznie w pamięci procesu (`ConcurrentHashMap` w komponentach `@Service`) — brak bazy danych, brak Spring Data JPA/Security/State Machine w tej iteracji. Zależności pom.xml: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-test`.

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

Zmienna `GOVERNANCE_SEED_MOCK_DATA` (domyślnie `true`, patrz `.env.example`/`deploy/compose.yml`, property `governance.seed-mock-data`) kontroluje, czy `ApprovalService` zasiewa przy starcie (`@PostConstruct`) wpisy zgodne z seedem `loan_application`/`loan_stage_transition` w `banking_backend` (`db/changelog/seed/002-loan.sql`):

- `11111111-1111-4111-8111-111111111001` — `LOAN_STAGE_TRANSITION`, `LN-80231` (jedyny wniosek, który w seedzie backendu faktycznie przeszedł `decision → contract`), status `APPROVED`, `requestedBy=PRC-10234`, `decidedBy=PRC-00001`.
- `22222222-2222-4222-8222-222222222002` — `LOAN_STAGE_TRANSITION`, `LN-80255` (nadal w `stage='decision'`, brak wiersza w `loan_stage_transition` — nikt jeszcze nie zdecydował), status `PENDING`, `requestedBy=PRC-20456`.

Ustawienie `GOVERNANCE_SEED_MOCK_DATA=false` startuje z pustą kolejką zatwierdzeń (jak przed R1.5). Stan nadal żyje tylko w pamięci procesu.

## Struktura kodu

```
src/main/java/robert_neat/governance_mock/
  audit/                          — AuditEvent, AuditService, AuditController
  approval/                       — ApprovalRequestRecord, ApprovalService, ApprovalController
  incident/                       — IctIncident, IctIncidentService, IctIncidentController
  casesla/                        — CaseSlaController (F8)
```

## Testy

`src/test/java/.../GovernanceMockApplicationTests.java` — `@SpringBootTest` (domyślny `webEnvironment=MOCK`, bez otwierania portu) + ręcznie budowany `MockMvc`, zamiast `@WebMvcTest`/`TestRestTemplate`, by nie dodawać zależności testowej poza `spring-boot-starter-test`. Testy pokrywają: odrzucenie samo-zatwierdzenia (409), poprawną decyzję + pojedynczy wpis audytowy, odrzucenie powtórnej decyzji (409), 404 dla nieznanego approval, eskalację SLA (<=0) z wpisem audytowym, brak eskalacji (>0) bez wpisu, 400 dla brakującego pola, cykl incydentu ICT (create → resolve → powtórne resolve = 409), 400 dla niekompletnego żądania approval, 400 z `ProblemDetail` dla nieprawidłowej wartości enum.

## Do uzupełnienia w innych częściach aplikacji

- `banking_backend` musi przekazywać stabilne `entityId` (np. id sprawy/wniosku/zajęcia) przy wywołaniach `POST /audit-events` i `POST /cases/{caseId}/sla-check`, aby wpisy audytowe i eskalacje można było korelować z encjami domenowymi.
- `POST /cases/{caseId}/sla-check` nie ma deduplikacji — wielokrotne wywołanie dla tej samej sprawy przy przekroczonym SLA tworzy kolejne wpisy `AUTO_ESCALATED`. `banking_backend` powinien wywoływać ten endpoint raz per przekroczenie progu (np. przy przejściu z `inProgress` do `escalated`), a nie przy każdym odświeżeniu widoku.
- Rozwiązanie zatwierdzeń (`decide`) porównuje `decidedBy` i `requestedBy` jako zwykłe stringi (`trim` + ignorowanie wielkości liter) — rozwiązane w R1.5: `banking_backend`'s `/approvals/{id}/decide` proxy ustawia `decidedBy` z `sub` JWT (zawsze `PRC-xxxxx`), nigdy z ciała żądania klienta.
- Następna sesja (druga iteracja): zamiana `ConcurrentHashMap` na trwały storage (Spring Data JPA + baza danych) z seederem — kontrakt REST pozostaje bez zmian, zmienia się tylko trwałość stanu.
- Brak autoryzacji/uwierzytelnienia na poziomie tego serwisu — zakładane jest, że sieć wewnętrzna (`banking-internal`) i `banking_backend` są jedynym wołającym; `identity_access_mock` odpowiada za RBAC na poziomie UI/`banking_backend`.
