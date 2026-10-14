# Planowane prace

- Dodać lekkie UI Thymeleaf (natywne fragmenty `th:insert`/`th:replace`, bez Layout Dialect) do zarządzania stanem serwisów mockowych wystawionych na porcie hosta: `external_institutions_gateway_mock`, `identity_access_mock` (Spring Boot). `card_processor_mock` (NestJS) pozostaje bez UI w tej iteracji — tylko REST API.
- Zaimplementować podstawowe przepływy (F1–F8 z `docs/banking_web_functions(concise).md`) w 5 serwisach mockowych: `card_processor_mock`, `credit_risk_mock`, `external_institutions_gateway_mock`, `governance_mock`, `identity_access_mock`. Runda 1: `governance_mock` + `credit_risk_mock`. Runda 2: `identity_access_mock` + `external_institutions_gateway_mock`. Runda 3: `card_processor_mock`.
- Każdy serwis dokumentuje swój kontrakt w `docs/<service>_contract/` oraz spostrzeżenia do uzupełnienia w innych częściach aplikacji w `docs/services/<service>.md`.
