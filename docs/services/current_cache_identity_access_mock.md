# Robocza lista kroków — identity_access_mock (sesja 1: stan w pamięci)

- [x] pom.xml: dodać spring-boot-starter-thymeleaf
- [x] application.properties: SERVER_PORT, CORS origin dla banking_web
- [x] common: ApiError, NotFoundException, ValidationException, ForbiddenException, UnauthorizedException, ConflictException, RestExceptionHandler
- [x] auth: DemoAccount/EmployeeAccount model, AccountStatus, RolePermissions (mapa rola->uprawnienia), AuthService (ConcurrentHashMap), AuthController (REST)
- [x] otp: OtpCode model, OtpPurpose, OtpService, OtpController (REST)
- [x] device: Device model, DeviceService, DeviceController (REST)
- [x] ebanking: EbankingAccessController (REST, zależny od OtpService)
- [x] CorsConfig (WebMvcConfigurer)
- [x] UI: fragments/header.html, fragments/footer.html, templates accounts/otp/devices/index.html + strona główna
- [x] UI: UiController (@Controller) + UiFooterAdvice dla portu
- [x] Testy jednostkowe/MockMvc (standalone) dla auth/otp/device/ebanking
- [x] mvnw test — zielone
- [x] docs/services/identity_access_mock.md
- [x] docs/identity_access_mock_contract/api.md

Brak pozycji w toku — iteracja zakończona.
