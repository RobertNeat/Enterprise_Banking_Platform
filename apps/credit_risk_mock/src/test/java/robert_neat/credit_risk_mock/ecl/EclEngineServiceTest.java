package robert_neat.credit_risk_mock.ecl;

import org.junit.jupiter.api.Test;
import robert_neat.credit_risk_mock.common.NotFoundException;
import robert_neat.credit_risk_mock.common.ValidationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EclEngineServiceTest {

    private final EclEngineService service = new EclEngineService();

    @Test
    void classifiesStage1ForLowDaysPastDue() {
        EclClassification result = service.classify(
                new ClassifyRequest("POS-1", 10, "BBB", new BigDecimal("10000")));

        assertThat(result.stage()).isEqualTo("stage1");
        assertThat(result.provision()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void classifiesStage2ForMidRangeDaysPastDue() {
        EclClassification result = service.classify(
                new ClassifyRequest("POS-2", 60, "BB-", new BigDecimal("10000")));

        assertThat(result.stage()).isEqualTo("stage2");
        assertThat(result.provision()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void classifiesStage3ForHighDaysPastDue() {
        EclClassification result = service.classify(
                new ClassifyRequest("POS-3", 120, "CCC", new BigDecimal("10000")));

        assertThat(result.stage()).isEqualTo("stage3");
        assertThat(result.provision()).isEqualTo(new BigDecimal("2500.00"));
    }

    @Test
    void boundaryDaysPastDueAreInclusiveOnLowerStage() {
        assertThat(service.classify(new ClassifyRequest("POS-4", 30, "A-", BigDecimal.TEN)).stage())
                .isEqualTo("stage1");
        assertThat(service.classify(new ClassifyRequest("POS-5", 31, "A-", BigDecimal.TEN)).stage())
                .isEqualTo("stage2");
        assertThat(service.classify(new ClassifyRequest("POS-6", 90, "A-", BigDecimal.TEN)).stage())
                .isEqualTo("stage2");
        assertThat(service.classify(new ClassifyRequest("POS-7", 91, "A-", BigDecimal.TEN)).stage())
                .isEqualTo("stage3");
    }

    @Test
    void latestReturnsMostRecentClassificationForPosition() {
        service.classify(new ClassifyRequest("POS-8", 10, "A", new BigDecimal("1000")));
        EclClassification updated = service.classify(new ClassifyRequest("POS-8", 95, "CCC", new BigDecimal("1000")));

        EclClassification latest = service.latest("POS-8");
        assertThat(latest.stage()).isEqualTo(updated.stage());
    }

    @Test
    void latestThrowsNotFoundForUnknownPosition() {
        assertThatThrownBy(() -> service.latest("UNKNOWN"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsNegativeDaysPastDue() {
        assertThatThrownBy(() -> service.classify(new ClassifyRequest("POS-9", -1, "BBB", BigDecimal.TEN)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsBlankCustomerRating() {
        assertThatThrownBy(() -> service.classify(new ClassifyRequest("POS-10", 10, " ", BigDecimal.TEN)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNonPositiveExposureAmount() {
        assertThatThrownBy(() -> service.classify(new ClassifyRequest("POS-11", 10, "BBB", BigDecimal.ZERO)))
                .isInstanceOf(ValidationException.class);
    }
}
