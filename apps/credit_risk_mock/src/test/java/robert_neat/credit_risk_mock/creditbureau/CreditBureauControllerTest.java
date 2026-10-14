package robert_neat.credit_risk_mock.creditbureau;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import robert_neat.credit_risk_mock.common.RestExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Standalone MockMvc setup: no web-slice test starter is on the classpath (not a declared dependency). */
class CreditBureauControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CreditBureauController controller = new CreditBureauController(new CreditBureauService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void scoreReturnsOkWithScoreAndDsti() throws Exception {
        mockMvc.perform(post("/credit-bureau/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":\"APP-100\",\"requestedAmount\":15000,\"segment\":\"retail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicantId").value("APP-100"))
                .andExpect(jsonPath("$.scoreResult").exists())
                .andExpect(jsonPath("$.dsti").exists())
                .andExpect(jsonPath("$.inquiryId").exists())
                .andExpect(jsonPath("$.queriedAt").exists());
    }

    @Test
    void scoreReturnsBadRequestForInvalidSegment() throws Exception {
        mockMvc.perform(post("/credit-bureau/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":\"APP-101\",\"requestedAmount\":15000,\"segment\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void historyReturnsEmptyListForUnknownApplicant() throws Exception {
        mockMvc.perform(get("/credit-bureau/history/UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void historyReflectsPreviousScoreCalls() throws Exception {
        mockMvc.perform(post("/credit-bureau/score")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"applicantId\":\"APP-102\",\"requestedAmount\":2000,\"segment\":\"business\"}"));

        mockMvc.perform(get("/credit-bureau/history/APP-102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicantId").value("APP-102"));
    }
}
