## Do zrobienia

1. **Wspólna klasa bazowa klientów REST (`*Client` + `*UnavailableException` → `503`)** — refaktor czystości kodu w `banking_backend`, bez zmiany zachowania zewnętrznego. Dotyczy 5 klas w `banking_backend/src/main/java/robert_neat/banking_backend/`:

   - `approvals/GovernanceClient.java` (245 linii), `auth/IdentityAccessClient.java` (161), `card/CardProcessorClient.java` (158), `creditrisk/CreditRiskClient.java` (88), `reporting/RegulatoryGatewayClient.java` (129).

   **Wzorzec wspólny w 4 z 5** (wszystkie oprócz `CardProcessorClient`): pole `RestClient restClient`, konstruktor `(RestClient.Builder, @Value baseUrl)`, prywatna `exchange(Supplier<T> call)` mapująca `HttpStatusCodeException`→`*Exception(status, message)` i `ResourceAccessException`→`*UnavailableException`, oraz `extractMessage(HttpStatusCodeException)` parsująca JSON (`GovernanceClient`/`RegulatoryGatewayClient` czytają pole `detail` najpierw — ich mocki zwracają RFC 9457 ProblemDetail; `IdentityAccessClient`/`CreditRiskClient` czytają tylko `message`). Łącznie ~70-80 linii niemal identycznego kodu rozproszonego po 5 klientach, plus 5 niemal identycznych 4-6-liniowych klas wyjątków `*UnavailableException`.

   **`CardProcessorClient` się różni**: dodatkowo rozpoznaje w `exchange()` status 503 jako symulowaną awarię procesora (osobny komunikat "outage simulated"), a `extractMessage` musi obsłużyć tablicę stringów (format walidacji NestJS, bo `card_processor_mock` jest NestJS, nie Spring Boot) — czyli potrzebuje nadpisania/hooka, nie jest 1:1 z resztą.

   **Szkic docelowej struktury** (do dopracowania przy implementacji, nie do ślepego kopiowania): abstrakcyjna klasa bazowa w `common/` (np. `AbstractMockClient`) z polem `restClient`, finalną metodą `exchange()` przyjmującą hooki `mapError(HttpStatusCode, String)`/`unavailable(String, Throwable)` dostarczane przez podklasy, oraz wspólną `extractMessage()` z parametrem `preferDetailField` (true dla Governance/RegulatoryGateway) i obsługą zarówno pola `message` jako string jak i jako tablicy (dla `CardProcessorClient`). Do decyzji: czy 5 klas `*UnavailableException` zostaje (czytelniejsze stack trace/asercje w testach) czy konsolidować do jednej `MockServiceUnavailableException(serviceName, ...)` — druga opcja wymaga przeglądu `common/RestExceptionHandler.java` (dziś 5 metod `@ExceptionHandler`, po jednej per typ).

   ROI: czystość kodu, mniejsza powtarzalność przy dodawaniu kolejnego klienta w przyszłości — bez zmiany funkcjonalnej, stąd osobno od reszty planu 80/20.

2. **Dokumentacja przepływów danych do testów manualnych** (`docs/system_flows/`) — pliki Markdown opisujące ścieżki klikalne w `banking_web` wraz z konkretnymi danymi wejściowymi (konta demo, ID encji z seeda, wartości formularzy) dla każdego przepływu mutującego (F2 kredyty, maker-checker, F8 sprawy, F4 karty, F5 e-banking, F3 ryzyko/windykacja, F6 zajęcia komornicze, F7 raportowanie) plus ścieżki negatywne (409/403/400/503→incydent ICT).

Poza powyższym, żadne inne obszary nie są dziś zidentyfikowane jako zaległe — poprzednia iteracja (CAS, dashboard live data, audit trail, panele historii, loan rejection/disburse, contact center, enforcement kind, CAS auto-escalation, Liquibase integrity fixes, lint, trwały storage we wszystkich 3 serwisach mockowych: `governance_mock`/`identity_access_mock`/`external_institutions_gateway_mock`) jest zamknięta i zacommitowana.

Zatwierdzanie zarejestrowanych użytkowników przez administratora i polskie formatowanie wartości pieniężnych/liczb zaimplementowane i zweryfikowane (lint/typecheck/testy/build), ale jeszcze NIE zacommitowane — czeka na przegląd.
