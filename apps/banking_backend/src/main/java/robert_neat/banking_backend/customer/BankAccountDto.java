package robert_neat.banking_backend.customer;

import java.math.BigDecimal;

public record BankAccountDto(
        String id,
        String maskedNumber,
        BigDecimal balance,
        BigDecimal blockedAmount) {
}
