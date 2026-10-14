package robert_neat.banking_backend.ebanking;

import java.math.BigDecimal;
import java.util.List;

public record EbankingUserDto(
        String id,
        String username,
        String customer,
        String channel,
        String accessStatus,
        BigDecimal dailyLimit,
        String lastLogin,
        List<String> permissions) {
}
