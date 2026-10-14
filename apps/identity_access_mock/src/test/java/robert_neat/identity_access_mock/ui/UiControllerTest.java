package robert_neat.identity_access_mock.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpRequestRequest;
import robert_neat.identity_access_mock.otp.OtpRequestResponse;
import robert_neat.identity_access_mock.otp.OtpService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full context needed to resolve real Thymeleaf templates/fragments (th:replace/th:insert), unlike the standalone REST tests. */
@SpringBootTest
class UiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OtpService otpService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void homeRendersWithHeaderAndFooter() throws Exception {
        mockMvc().perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("identity_access_mock")));
    }

    @Test
    void otpPageRendersTheGeneratedCode() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("PRC-30678", OtpPurpose.UNLOCK_ACCESS));
        String code = otpService.getOrThrow(response.otpId()).getCode();

        mockMvc().perform(get("/otp"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(code)));
    }

    @Test
    void accountsPageListsSeededAccounts() throws Exception {
        mockMvc().perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PRC-00001")));
    }

    @Test
    void devicesPageRendersSeededDevices() throws Exception {
        mockMvc().perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DEV-0001")));
    }
}
