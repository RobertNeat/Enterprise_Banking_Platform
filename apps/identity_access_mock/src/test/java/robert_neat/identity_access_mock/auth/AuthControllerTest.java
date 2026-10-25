package robert_neat.identity_access_mock.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Promoted from standalone MockMvc (new AuthService()) to @SpringBootTest, since AuthService now
 * needs a JPA repository/EntityManager. Relies on the Liquibase seed (6 demo accounts), so it only
 * gets its own isolated H2 url rather than clearing the employee_account table — the one test that
 * mutates state (registerCreatesPendingApprovalAccountAndBlocksLogin) cleans up its own row in
 * @AfterEach so the accounts-count assertion below stays correct regardless of execution order.
 */
@SpringBootTest
@TestPropertySource(
        properties =
                "spring.datasource.url=jdbc:h2:mem:identity_access_auth_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EmployeeAccountRepository accountRepository;

    @Autowired
    private AuthSessionRepository sessionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void cleanUp() {
        // approve+login tests create a session row (FK on employee_id) before the account itself
        // is deleted — must go first or the account delete fails with a referential integrity error.
        sessionRepository.findAll().stream()
                .filter(session -> session.getEmployeeId().equals("PRC-99999"))
                .forEach(sessionRepository::delete);
        accountRepository.findByEmployeeId("PRC-99999").ifPresent(accountRepository::delete);
    }

    @Test
    void loginReturnsTokenRoleAndPermissionsForDemoAccount() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"PRC-00001\",\"password\":\"Admin12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.employeeId").value("PRC-00001"))
                .andExpect(jsonPath("$.role").value("Administrator systemu"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions").isNotEmpty());
    }

    @Test
    void loginReturnsUnauthorizedForWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"PRC-00001\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void registerCreatesPendingApprovalAccountAndBlocksLogin() throws Exception {
        String body = "{"
                + "\"personal\":{\"firstName\":\"Jan\",\"lastName\":\"Kowalski\",\"email\":\"j.k@example.com\",\"phone\":\"123456789\"},"
                + "\"employment\":{\"employeeId\":\"PRC-99999\",\"department\":\"IT\",\"branch\":\"WAW\",\"position\":\"Dev\",\"supervisor\":\"Boss\",\"startDate\":\"2026-10-14T00:00:00.000Z\"},"
                + "\"industry\":{\"businessLines\":[\"it\"],\"certifications\":\"\",\"licenseNumber\":\"\"},"
                + "\"account\":{\"password\":\"Passw0rd1\",\"confirmPassword\":\"Passw0rd1\",\"acceptPolicy\":true}"
                + "}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value("PRC-99999"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"PRC-99999\",\"password\":\"Passw0rd1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountsListsSeededDemoAccountsWithoutPasswords() throws Exception {
        mockMvc.perform(get("/auth/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void logoutIsIdempotentForUnknownToken() throws Exception {
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer unknown-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void approveActivatesAPendingAccountWithTheGivenRoleAndAllowsLogin() throws Exception {
        registerPendingAccount();
        String id = accountRepository.findByEmployeeId("PRC-99999").orElseThrow().getId();

        mockMvc.perform(post("/auth/accounts/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"Doradca kredytowy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("Doradca kredytowy"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"PRC-99999\",\"password\":\"Passw0rd1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Doradca kredytowy"));
    }

    @Test
    void approveRejectsAnUnknownRole() throws Exception {
        registerPendingAccount();
        String id = accountRepository.findByEmployeeId("PRC-99999").orElseThrow().getId();

        mockMvc.perform(post("/auth/accounts/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"Nieznana rola\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectSetsStatusToRejectedAndKeepsLoginBlocked() throws Exception {
        registerPendingAccount();
        String id = accountRepository.findByEmployeeId("PRC-99999").orElseThrow().getId();

        mockMvc.perform(post("/auth/accounts/" + id + "/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"PRC-99999\",\"password\":\"Passw0rd1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not active: REJECTED"));
    }

    @Test
    void approveReturnsNotFoundForAnUnknownId() throws Exception {
        mockMvc.perform(post("/auth/accounts/does-not-exist/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"Doradca kredytowy\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveReturnsConflictForAnAlreadyActiveAccount() throws Exception {
        mockMvc.perform(get("/auth/accounts"))
                .andExpect(status().isOk());
        EmployeeAccount admin = accountRepository.findByEmployeeId("PRC-00001").orElseThrow();

        mockMvc.perform(post("/auth/accounts/" + admin.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"Doradca kredytowy\"}"))
                .andExpect(status().isConflict());
    }

    private void registerPendingAccount() throws Exception {
        String body = "{"
                + "\"personal\":{\"firstName\":\"Jan\",\"lastName\":\"Kowalski\",\"email\":\"j.k@example.com\",\"phone\":\"123456789\"},"
                + "\"employment\":{\"employeeId\":\"PRC-99999\",\"department\":\"IT\",\"branch\":\"WAW\",\"position\":\"Dev\",\"supervisor\":\"Boss\",\"startDate\":\"2026-10-14T00:00:00.000Z\"},"
                + "\"industry\":{\"businessLines\":[\"it\"],\"certifications\":\"\",\"licenseNumber\":\"\"},"
                + "\"account\":{\"password\":\"Passw0rd1\",\"confirmPassword\":\"Passw0rd1\",\"acceptPolicy\":true}"
                + "}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
