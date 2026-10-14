package robert_neat.banking_backend.cases;

/** POST /cases/{id}/escalate response — whether this call actually escalated the case. */
public record EscalateCaseResponse(CustomerCaseDto customerCase, boolean escalated) {
}
