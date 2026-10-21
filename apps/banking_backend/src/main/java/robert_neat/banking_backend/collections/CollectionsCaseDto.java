package robert_neat.banking_backend.collections;

import java.math.BigDecimal;

public record CollectionsCaseDto(
    String id,
    String customer,
    String loanId,
    int daysPastDue,
    BigDecimal outstandingAmount,
    String stage) {}
