# Robocza lista kroków — governance_mock (bieżąca sesja)

- [x] Modele domenowe (records): ApprovalRequest, ApprovalDecision request, AuditEvent, IctIncident
- [x] Enum: ApprovalStatus (PENDING/APPROVED/REJECTED), Decision (APPROVED/REJECTED), Severity (LOW/MEDIUM/HIGH), IncidentStatus (OPEN/RESOLVED)
- [x] AuditLogService (in-memory ConcurrentHashMap) + AuditController (POST /audit-events, GET /audit-events z filtrem entityType/entityId)
- [x] ApprovalService (in-memory) + ApprovalController (POST /approvals, POST /approvals/{id}/decide, GET /approvals z filtrem status, GET /approvals/{id}) — decide() dopisuje audit event, 409 gdy decidedBy == requestedBy
- [x] IctIncidentService (in-memory) + IctIncidentController (POST /ict-incidents, GET /ict-incidents, POST /ict-incidents/{id}/resolve)
- [x] CaseSlaController (F8): POST /cases/{caseId}/sla-check — eskalacja gdy slaRemainingHours <= 0, dopisuje audit event AUTO_ESCALATED
- [x] Walidacja wejścia (brakujące pola -> 400) + obsługa 404 dla nieznalezionych id (ApiError @RestControllerAdvice + ResponseStatusException)
- [x] Testy jednostkowe/integracyjne (MockMvc, @SpringBootTest MOCK) — 10/10 zielone
- [x] docs/governance_mock_contract/api.md — kontrakt API
- [x] docs/services/governance_mock.md — stan bieżący + spostrzeżenia dla innych modułów
- [x] mvnw.cmd test — zielone testy

Wszystkie kroki tej sesji zakończone.
