package robert_neat.identity_access_mock.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import robert_neat.identity_access_mock.common.RestExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Standalone MockMvc setup: no web-slice test starter is on the classpath (not a declared dependency). */
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
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
                .andExpect(jsonPath("$.permissions").isArray());
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
}
