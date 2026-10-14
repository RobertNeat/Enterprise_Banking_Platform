package robert_neat.banking_backend.card;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardAuthorizationDto(
        String id,
        String cardId,
        String merchant,
        BigDecimal amount,
        LocalDate date,
        String processor,
        boolean approved) {
}
