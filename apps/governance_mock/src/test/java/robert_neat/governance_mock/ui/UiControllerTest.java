package robert_neat.governance_mock.ui;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full context needed to resolve real Thymeleaf templates/fragments (th:replace/th:insert), unlike the standalone REST tests. */
@SpringBootTest
class UiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void homeRendersWithHeaderAndFooter() throws Exception {
        mockMvc().perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("app-header")))
                .andExpect(content().string(containsString("app-footer")));
    }

    @Test
    void homePageLinksTheStylesheets() throws Exception {
        mockMvc().perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/css/theme.css\"")))
                .andExpect(content().string(containsString("href=\"/css/theme-vars.css\"")));
    }

    @Test
    void themeCssIsServed() throws Exception {
        mockMvc().perform(get("/css/theme.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".card")));
    }

    @Test
    void themeVarsCssIsServed() throws Exception {
        mockMvc().perform(get("/css/theme-vars.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--primary-color")));
    }
}
