package robert_neat.external_institutions_gateway_mock.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full context needed to resolve real Thymeleaf templates/fragments (th:replace), unlike the
 * standalone REST tests. Asserts the layout fragment and static CSS actually render/serve,
 * not just that the controller returns 200 -- a bad fragment selector would only surface here.
 */
@SpringBootTest
@TestPropertySource(
        properties =
                "spring.datasource.url=jdbc:h2:mem:gateway_ui_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class UiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void homeRendersIndexPageWithHeaderAndFooter() throws Exception {
        mockMvc().perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("external_institutions_gateway_mock")))
                .andExpect(content().string(containsString("app-header")))
                .andExpect(content().string(containsString("app-footer")))
                .andExpect(content().string(containsString("port 10523")));
    }

    @Test
    void enforcementPageUsesLayoutAndStaticCss() throws Exception {
        mockMvc().perform(get("/ui/enforcement"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/css/theme.css\"")))
                .andExpect(content().string(containsString("href=\"/css/theme-vars.css\"")))
                .andExpect(content().string(containsString("class=\"app-header\"")))
                .andExpect(content().string(containsString("class=\"app-footer\"")));
    }

    @Test
    void reportsPageUsesLayout() throws Exception {
        mockMvc().perform(get("/ui/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"app-header\"")))
                .andExpect(content().string(containsString("class=\"app-footer\"")));
    }

    @Test
    void fatcaPageUsesLayout() throws Exception {
        mockMvc().perform(get("/ui/fatca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"app-header\"")))
                .andExpect(content().string(containsString("class=\"app-footer\"")));
    }

    @Test
    void themeCssIsServedAndContainsCardStyle() throws Exception {
        mockMvc().perform(get("/css/theme.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".card")));
    }

    @Test
    void themeVarsCssIsServedAndDefinesPrimaryColor() throws Exception {
        mockMvc().perform(get("/css/theme-vars.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--primary-color")));
    }

    @Test
    void formPostRedirectsBackToEnforcementWithoutLeakingModelAttributes() throws Exception {
        mockMvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/ui/enforcement")
                        .param("bailiff", "Komornik CSS Test")
                        .param("caseNumber", "KM CSS/01")
                        .param("customer", "CUST-CSS")
                        .param("blockedAmount", "10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/ui/enforcement"));
    }
}
