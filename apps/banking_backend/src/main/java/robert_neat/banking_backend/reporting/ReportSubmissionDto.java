package robert_neat.banking_backend.reporting;

import java.time.Instant;

public record ReportSubmissionDto(
        String gatewaySubmissionId,
        String gatewayStatus,
        String validationErrors,
        Instant submittedAt,
        Instant resolvedAt) {
}
