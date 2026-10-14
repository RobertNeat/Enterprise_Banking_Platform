package robert_neat.external_institutions_gateway_mock.fatca;

import java.time.Instant;

public record FatcaCrsExport(String reportId, String period, Instant generatedAt, String xml) {
}
