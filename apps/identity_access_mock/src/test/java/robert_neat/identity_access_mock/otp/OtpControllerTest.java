package robert_neat.identity_access_mock.otp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Promoted from standalone MockMvc (new OtpService()) to @SpringBootTest, since OtpService now
 * needs a JPA repository/EntityManager. The otp_code table isn't seeded by Liquibase, so this test
 * can safely clear it in @BeforeEach.
 */
@SpringBootTest
@TestPropertySource(
        properties =
                "spring.datasource.url=jdbc:h2:mem:identity_access_otp_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class OtpControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        otpCodeRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void requestReturnsOtpIdAndExpiryWithoutCode() throws Exception {
        mockMvc.perform(post("/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":\"PRC-30678\",\"purpose\":\"UNLOCK_ACCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpId").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void verifyReturnsTrueForCorrectCodeAndFalseForUnknownOtp() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("PRC-30678", OtpPurpose.UNLOCK_ACCESS));
        String code = otpService.getOrThrow(response.otpId()).getCode();

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"unknown-id\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));
    }
}
