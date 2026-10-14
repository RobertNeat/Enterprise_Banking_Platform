package robert_neat.external_institutions_gateway_mock.fatca;

import java.util.List;

public record FatcaCrsExportRequest(
        String reportId,
        String period,
        String recipient,
        List<FatcaCrsRecord> records
) {
}
