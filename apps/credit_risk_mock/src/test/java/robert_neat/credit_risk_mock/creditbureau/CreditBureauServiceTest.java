package robert_neat.credit_risk_mock.creditbureau;

import org.junit.jupiter.api.Test;
import robert_neat.credit_risk_mock.common.ValidationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditBureauServiceTest {

    private final CreditBureauService service = new CreditBureauService();

    @Test
    void scoreIsDeterministicForSameApplicant() {
        ScoreRequest request = new ScoreRequest("APP-1", new BigDecimal("10000"), "retail");

        CreditBureauInquiry first = service.score(request);
        CreditBureauInquiry second = service.score(request);

        assertThat(first.scoreResult()).isEqualTo(second.scoreResult());
        assertThat(first.dsti()).isEqualTo(second.dsti());
        assertThat(first.scoreResult()).isBetween(0, 100);
        assertThat(first.dsti()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(first.dsti()).isLessThanOrEqualTo(BigDecimal.ONE);
    }

    @Test
    void higherRequestedAmountCorrelatesWithHigherOrEqualDsti() {
        CreditBureauInquiry small = service.score(new ScoreRequest("APP-2", new BigDecimal("1000"), "retail"));
        CreditBureauInquiry large = service.score(new ScoreRequest("APP-2", new BigDecimal("900000"), "retail"));

        assertThat(large.dsti()).isGreaterThanOrEqualTo(small.dsti());
    }

    @Test
    void recordsHistoryPerApplicant() {
        ScoreRequest request = new ScoreRequest("APP-3", new BigDecimal("5000"), "business");

        service.score(request);
        service.score(request);

        List<CreditBureauInquiry> history = service.history("APP-3");
        assertThat(history).hasSize(2);
    }

    @Test
    void historyIsEmptyForUnknownApplicant() {
        assertThat(service.history("UNKNOWN")).isEmpty();
    }

    @Test
    void rejectsBlankApplicantId() {
        assertThatThrownBy(() -> service.score(new ScoreRequest(" ", BigDecimal.TEN, "retail")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.score(new ScoreRequest("APP-4", BigDecimal.ZERO, "retail")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsUnknownSegment() {
        assertThatThrownBy(() -> service.score(new ScoreRequest("APP-5", BigDecimal.TEN, "corporate")))
                .isInstanceOf(ValidationException.class);
    }
}
