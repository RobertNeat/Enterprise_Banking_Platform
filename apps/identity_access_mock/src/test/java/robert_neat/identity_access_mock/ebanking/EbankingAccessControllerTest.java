package robert_neat.identity_access_mock.ebanking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import robert_neat.identity_access_mock.otp.OtpCodeRepository;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpRequestRequest;
import robert_neat.identity_access_mock.otp.OtpRequestResponse;
import robert_neat.identity_access_mock.otp.OtpService;
import robert_neat.identity_access_mock.otp.OtpVerifyRequest;

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
                "spring.datasource.url=jdbc:h2:mem:identity_access_ebanking_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class EbankingAccessControllerTest {

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
