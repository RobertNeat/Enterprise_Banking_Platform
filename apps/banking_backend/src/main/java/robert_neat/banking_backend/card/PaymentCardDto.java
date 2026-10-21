package robert_neat.banking_backend.card;

import java.math.BigDecimal;

public record PaymentCardDto(
    String id,
    String holder,
    String maskedNumber,
    String network,
    String product,
    String status,
    String expiry,
    BigDecimal limitDaily,
    BigDecimal limitAtm) {}
