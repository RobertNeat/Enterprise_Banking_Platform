package robert_neat.banking_backend.reporting;

import java.time.LocalDate;

public record RegulatoryReportDto(
        String id,
        String recipient,
        String name,
        String period,
        LocalDate dueDate,
        String status) {
}
