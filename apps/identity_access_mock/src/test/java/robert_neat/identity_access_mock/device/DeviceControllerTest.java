package robert_neat.identity_access_mock.device;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Promoted from standalone MockMvc (new OtpService()/new DeviceService()) to @SpringBootTest, since
 * both services now need a JPA repository/EntityManager. Relies on the Liquibase device seed
 * (DEV-0001/0002/0003), so it only gets its own isolated H2 url and clears the (unseeded) otp_code
 * table in @BeforeEach rather than relying on a fresh ConcurrentHashMap per test (old pattern).
 */
@SpringBootTest
@TestPropertySource(
        properties =
                "spring.datasource.url=jdbc:h2:mem:identity_access_device_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class DeviceControllerTest {

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
    void byOwnerReturnsSeededDevices() throws Exception {
        mockMvc.perform(get("/devices/PRC-30678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("DEV-0001"));
    }

    @Test
    void unbindMarksDeviceUntrusted() throws Exception {
        mockMvc.perform(post("/devices/DEV-0001/unbind"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/devices/PRC-30678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trusted").value(false));
    }

    @Test
    void resetWithoutVerifiedOtpReturnsForbidden() throws Exception {
        mockMvc.perform(post("/devices/DEV-0001/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"missing\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetWithVerifiedOtpSucceeds() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("PRC-30678", OtpPurpose.RESET_DEVICE));
        String code = otpService.getOrThrow(response.otpId()).getCode();
        otpService.verify(new OtpVerifyRequest(response.otpId(), code));

        mockMvc.perform(post("/devices/DEV-0001/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetCannotReplayTheSameVerifiedOtp() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("PRC-30678", OtpPurpose.RESET_DEVICE));
        String code = otpService.getOrThrow(response.otpId()).getCode();
        otpService.verify(new OtpVerifyRequest(response.otpId(), code));

        mockMvc.perform(post("/devices/DEV-0001/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/devices/DEV-0001/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otpId\":\"" + response.otpId() + "\"}"))
                .andExpect(status().isForbidden());
    }
}
