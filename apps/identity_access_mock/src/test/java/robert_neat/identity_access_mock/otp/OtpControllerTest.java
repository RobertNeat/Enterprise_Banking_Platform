package robert_neat.identity_access_mock.otp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import robert_neat.identity_access_mock.common.RestExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OtpControllerTest {

    private MockMvc mockMvc;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        OtpController controller = new OtpController(otpService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
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
