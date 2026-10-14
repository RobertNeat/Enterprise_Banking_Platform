package robert_neat.external_institutions_gateway_mock.fatca;

import java.math.BigDecimal;

public record FatcaCrsRecord(String customer, String taxId, BigDecimal balance) {
}
