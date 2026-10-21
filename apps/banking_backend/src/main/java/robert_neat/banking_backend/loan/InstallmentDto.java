package robert_neat.banking_backend.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentDto(
    int no, LocalDate dueDate, BigDecimal principal, BigDecimal interest, String status) {}
