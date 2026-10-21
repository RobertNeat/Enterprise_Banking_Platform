package robert_neat.banking_backend.cases;

import java.time.LocalDate;

public record CustomerCaseDto(
    String id,
    String subject,
    String customer,
    String owner,
    String priority,
    LocalDate statutoryDeadline,
    long slaRemainingHours,
    String status) {}
