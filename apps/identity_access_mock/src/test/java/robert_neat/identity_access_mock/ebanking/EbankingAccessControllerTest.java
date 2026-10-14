package robert_neat.identity_access_mock.ebanking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import robert_neat.identity_access_mock.common.RestExceptionHandler;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpRequestRequest;
import robert_neat.identity_access_mock.otp.OtpRequestResponse;
import robert_neat.identity_access_mock.otp.OtpService;
import robert_neat.identity_access_mock.otp.OtpVerifyRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EbankingAccessControllerTest {

    private MockMvc mockMvc;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        EbankingAccessController controller = new EbankingAccessController(otpService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void unlockWithoutVerifiedOtpReturnsForbidden() throws Exception {
        mockMvc.perform(post("/ebanking-access/EB-1001/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"missing\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unlockWithVerifiedOtpSucceeds() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("EB-1001", OtpPurpose.UNLOCK_ACCESS));
        String code = otpService.getOrThrow(response.otpId()).getCode();
        otpService.verify(new OtpVerifyRequest(response.otpId(), code));

        mockMvc.perform(post("/ebanking-access/EB-1001/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlocked").value(true));
    }

    @Test
    void unlockCannotReplayTheSameVerifiedOtp() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("EB-1001", OtpPurpose.UNLOCK_ACCESS));
        String code = otpService.getOrThrow(response.otpId()).getCode();
        otpService.verify(new OtpVerifyRequest(response.otpId(), code));

        mockMvc.perform(post("/ebanking-access/EB-1001/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/ebanking-access/EB-1001/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\"}"))
                .andExpect(status().isForbidden());
    }
}
