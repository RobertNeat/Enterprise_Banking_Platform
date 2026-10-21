package robert_neat.banking_backend.reporting;

import java.time.Instant;

/** Mirrors external_institutions_gateway_mock's POST /enforcement/{id}/transfer response shape. */
public record EnforcementTransferResponse(String id, String status, Instant confirmedAt) {}
