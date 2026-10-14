package robert_neat.identity_access_mock.device;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerTest {

    private MockMvc mockMvc;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
        DeviceService deviceService = new DeviceService(otpService);
        DeviceController controller = new DeviceController(deviceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
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
