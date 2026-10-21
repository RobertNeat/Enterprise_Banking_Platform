package robert_neat.banking_backend.reporting;

import java.time.Instant;
import java.util.List;

/** Mirrors external_institutions_gateway_mock's regulatory-gateway submission JSON shape. */
public record GatewaySubmissionResponse(
    String submissionId,
    String reportId,
    String recipient,
    String name,
    String period,
    String status,
    List<String> validationErrors,
    Instant submittedAt,
    Instant resolvedAt) {}
