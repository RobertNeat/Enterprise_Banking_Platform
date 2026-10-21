package robert_neat.banking_backend.customer;

public record CustomerDto(
    String id, String fullName, String segment, String taxId, String riskRating) {}
