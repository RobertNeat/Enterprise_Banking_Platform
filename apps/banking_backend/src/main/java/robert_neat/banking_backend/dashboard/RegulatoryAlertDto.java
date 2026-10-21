package robert_neat.banking_backend.dashboard;

import java.time.LocalDate;

public record RegulatoryAlertDto(String id, String recipient, String status, LocalDate dueDate) {}
