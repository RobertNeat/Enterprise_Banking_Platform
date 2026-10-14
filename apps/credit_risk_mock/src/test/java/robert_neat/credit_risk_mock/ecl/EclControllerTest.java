package robert_neat.credit_risk_mock.ecl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import robert_neat.credit_risk_mock.common.RestExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Standalone MockMvc setup: no web-slice test starter is on the classpath (not a declared dependency). */
class EclControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EclController controller = new EclController(new EclEngineService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void classifyReturnsStageAndProvision() throws Exception {
        mockMvc.perform(post("/ecl/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionId\":\"POS-200\",\"daysPastDue\":15,\"customerRating\":\"BBB\",\"exposureAmount\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId").value("POS-200"))
                .andExpect(jsonPath("$.stage").value("stage1"))
                .andExpect(jsonPath("$.provision").value(100.00))
                .andExpect(jsonPath("$.classifiedAt").exists());
    }

    @Test
    void classifyReturnsBadRequestForMissingPositionId() throws Exception {
        mockMvc.perform(post("/ecl/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionId\":\"\",\"daysPastDue\":15,\"customerRating\":\"BBB\",\"exposureAmount\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void positionsReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/ecl/positions/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void positionsReturnsLatestClassification() throws Exception {
        mockMvc.perform(post("/ecl/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"positionId\":\"POS-201\",\"daysPastDue\":95,\"customerRating\":\"CCC\",\"exposureAmount\":4000}"));

        mockMvc.perform(get("/ecl/positions/POS-201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("stage3"))
                .andExpect(jsonPath("$.provision").value(1000.00));
    }
}
