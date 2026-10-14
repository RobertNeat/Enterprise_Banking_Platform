# governance_mock — stan bieżący

Serwis Spring Boot 4.1.1 (Java 25), wewnętrzny (bez portu na hoście), wołany wyłącznie przez `banking_backend`. Port wewnętrzny: `10524` (`server.port` w `application.properties`, nadpisywalny przez env `SERVER_PORT` — zmienna środowiskowa ma priorytet nad plikiem properties, więc `deploy/compose.yml` działa bez zmian w kodzie).

Stan przechowywany wyłącznie w pamięci procesu (`ConcurrentHashMap` w komponentach `@Service`) — brak bazy danych, brak Spring Data JPA/Security/State Machine w tej iteracji. Zależności pom.xml bez zmian: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-test`.

## Zaimplementowane endpointy

Pełny kontrakt JSON: `docs/governance_mock_contract/api.md`.

- **Approval queue (maker-checker)**: `POST /approvals`, `GET /approvals` (filtr `status`), `GET /approvals/{id}`, `POST /approvals/{id}/decide`. Decyzja wymaga `decidedBy != requestedBy` (409 w przeciwnym razie) i statusu `PENDING` (409 jeśli już zdecydowany). Każda decyzja automatycznie dopisuje wpis do audit log.
- **Audit log**: `POST /audit-events`, `GET /audit-events` (filtry `entityType`, `entityId`).
- **ICT incident registry (DORA)**: `POST /ict-incidents`, `GET /ict-incidents`, `POST /ict-incidents/{id}/resolve` (409 jeśli już `RESOLVED`).
- **F8 — SLA/eskalacja spraw klientów**: `POST /cases/{caseId}/sla-check` — eskalacja automatyczna (`escalated: true` + wpis audytowy `AUTO_ESCALATED`) gdy `slaRemainingHours <= 0`.
- **Health**: `GET /actuator/health` (Spring Boot Actuator, bez dodatkowego endpointu `/health`).

Błędy walidacyjne/konfliktowe (400/404/409) zwracane jako `ProblemDetail` (RFC 9457) — włączone przez `spring.mvc.problemdetails.enabled=true`, obejmuje zarówno ręcznie rzucane `ResponseStatusException`, jak i błędy deserializacji JSON (np. nieznana wartość enum) obsługiwane przez domyślny mechanizm Spring MVC.

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
- Rozwiązanie zatwierdzeń (`decide`) porównuje `decidedBy` i `requestedBy` jako zwykłe stringi (`trim` + ignorowanie wielkości liter) — `banking_backend` powinien przekazywać identyfikator pracownika w stabilnym formacie (np. `PRC-00000`), zgodnym z `ACC-01`, żeby porównanie było sensowne.
- `GET /ict-incidents` nie ma filtra po `status`/`severity` — jeśli pulpit (`DSH-05`) będzie potrzebował tylko otwartych incydentów o wysokiej istotności, filtrowanie trzeba będzie dodać po stronie `banking_backend` lub rozszerzyć ten endpoint w kolejnej iteracji.
- Następna sesja (druga iteracja): zamiana `ConcurrentHashMap` na trwały storage (Spring Data JPA + baza danych) z seederem — kontrakt REST pozostaje bez zmian, zmienia się tylko trwałość stanu.
- Brak autoryzacji/uwierzytelnienia na poziomie tego serwisu — zakładane jest, że sieć wewnętrzna (`banking-internal`) i `banking_backend` są jedynym wołającym; `identity_access_mock` odpowiada za RBAC na poziomie UI/`banking_backend`.
