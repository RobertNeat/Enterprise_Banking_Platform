package robert_neat.banking_backend.contactcenter;

public record CustomerProfileDto(
    String name,
    String segment,
    long loansCount,
    long cardsCount,
    String riskRating,
    long openCases) {}
