package robert_neat.external_institutions_gateway_mock.regulatory;

import java.time.Instant;
import java.util.List;

public record RegulatorySubmission(
        String submissionId,
        String reportId,
        String recipient,
        String name,
        String period,
        SubmissionStatus status,
        List<String> validationErrors,
        Instant submittedAt,
        Instant resolvedAt
) {
    public RegulatorySubmission withResolution(SubmissionStatus status, List<String> validationErrors, Instant resolvedAt) {
        return new RegulatorySubmission(submissionId, reportId, recipient, name, period,
                status, validationErrors, submittedAt, resolvedAt);
    }
}
