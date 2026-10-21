package robert_neat.banking_backend.dashboard;

public record RecentCaseDto(
    String id, String subject, String customer, String status, String owner) {}
