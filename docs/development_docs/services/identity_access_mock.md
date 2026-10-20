# identity_access_mock

## Stan bieżący

Spring Boot 4.1.1, Java 25, port domyślny `10525` (env `SERVER_PORT`), eksponowany na hoście — wołany
bezpośrednio z przeglądarki (banking_web). Stan trzymany wyłącznie w pamięci procesu (`ConcurrentHashMap`
w serwisach `@Service`, singleton). Brak bazy danych, Spring Security i Spring Authorization Server —
to zostanie dodane w kolejnej sesji.

Konsoliduje trzy mockowane funkcje: RBAC (rola → uprawnienia), OTP/MFA (PSD2 SCA), rejestr zaufanych
urządzeń mobilnych. Realizuje F1 (logowanie + uprawnienia) i F5 (odblokowanie dostępu e-banking z silnym
uwierzytelnieniem) z `docs/banking_web_functions(concise).md`.

### Moduły

- **auth** — konta pracowników (`EmployeeAccount`), logowanie/wylogowanie, rejestracja, statyczna mapa
  rola→uprawnienia (`RolePermissions`). 6 kont demo zgodnych z `DemoAccount` z banking_web
  (`PRC-00001`…`PRC-50123`). Token sesji to zwykły UUID (nie JWT), przekazywany w `Authorization: Bearer <token>`.
  `RolePermissions` jest świadomie bez hierarchii ("no persistence, no hierarchy") — hierarchia
  (`seniorityLevel`) żyje wyłącznie w `banking_backend`'s `auth/RoleCode.java` (R1.5). Permission
  `"approvals"` (dodana w R1.5, tylko dla `ADMIN`/`COMPLIANCE`) gatuje widok kolejki zatwierdzeń
  (maker-checker) w `banking_web` — to osobny mechanizm od hierarchii, nie łamie konwencji
  "no hierarchy" (permissions i seniority to różne osie).
- **otp** — generowanie i weryfikacja 6-cyfrowych kodów OTP (TTL 5 minut, jednorazowe — po pozytywnej
  weryfikacji kod jest oznaczany jako zużyty). Kod nigdy nie wraca w odpowiedzi REST — jest widoczny
  tylko w panelu operatorskim (`/otp`).
- **device** — rejestr urządzeń mobilnych (`deviceId`, właściciel = employeeId lub customerId/userId
  e-bankingu, model, `lastSeenAt`, `trusted`). Reset urządzenia wymaga wcześniej zweryfikowanego OTP
  o celu `RESET_DEVICE`.
- **ebanking** — `POST /ebanking-access/{userId}/unlock` potwierdza odblokowanie dostępu po weryfikacji
  OTP o celu `UNLOCK_ACCESS`. Sam byt `EbankingUser` (status `accessStatus`) żyje w `banking_backend` —
  ten serwis tylko certyfikuje, że silne uwierzytelnienie się powiodło.

### Panel operatorski (Thymeleaf)

Strony `@Controller` (nie REST) pod `/`, `/accounts`, `/otp`, `/devices`, `/ebanking` — czytają z tych
samych serwisów w pamięci co REST API. Wspólny layout (`templates/fragments/layout.html`) z natywnymi
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
- **banking_backend**: integracja `POST /auth/login` jako źródło tokenu/uprawnień dla sesji pracownika;
  obecnie frontend nawiguje lokalnie bez wołania żadnego backendu auth.
- **banking_backend / EbankingUser**: po pozytywnym `POST /ebanking-access/{userId}/unlock` backend
  powinien przestawić `accessStatus` z `locked`/`pendingReset` na `active` we własnej encji.
- **governance_mock**: rejestracja nowego konta (`PENDING_APPROVAL`) powinna trafiać do kolejki
  zatwierdzeń (`approval queue`) w `governance_mock`; obecnie to tylko status bez integracji.
- **Przyszła sesja (ta sama usługa)**: migracja stanu z `ConcurrentHashMap` na trwały storage
  (Spring Data JPA) + seeder bazy danych; ewentualne przejście na Spring Security / Spring
  Authorization Server zamiast własnego tokenu UUID (zgodnie ze starą specyfikacją techniczną).
