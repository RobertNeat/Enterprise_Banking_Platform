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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    @Test
    void themeCssIsServedAndNonEmpty() throws Exception {
        mockMvc().perform(get("/css/theme.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("app-header")));
    }

    @Test
    void themeVarsCssIsServedAndNonEmpty() throws Exception {
        mockMvc().perform(get("/css/theme-vars.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--primary-color")));
    }

    @Test
    void homePageLinksTheStylesheetsAndUsesSharedLayout() throws Exception {
        mockMvc().perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/theme.css\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/theme-vars.css\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"app-header\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"app-footer\"")));
    }

    @Test
    void ebankingPageRenders() throws Exception {
        mockMvc().perform(get("/ebanking"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Odblokowanie dostępu e-banking")));
    }

    @Test
    void unlockUiSucceedsWithCorrectCode() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("CUST-1", OtpPurpose.UNLOCK_ACCESS));
        String code = otpService.getOrThrow(response.otpId()).getCode();

        mockMvc().perform(post("/ebanking/unlock-ui")
                        .param("userId", "CUST-1")
                        .param("otpId", response.otpId())
                        .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ebanking"))
                .andExpect(flash().attribute("result", org.hamcrest.Matchers.containsString("odblokowany")));
    }

    @Test
    void unlockUiFailsWithWrongCode() throws Exception {
        OtpRequestResponse response = otpService.requestOtp(new OtpRequestRequest("CUST-2", OtpPurpose.UNLOCK_ACCESS));

        mockMvc().perform(post("/ebanking/unlock-ui")
                        .param("userId", "CUST-2")
                        .param("otpId", response.otpId())
                        .param("code", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ebanking"))
                .andExpect(flash().attribute("result", org.hamcrest.Matchers.containsString("nie powiodło się")));
    }
}
