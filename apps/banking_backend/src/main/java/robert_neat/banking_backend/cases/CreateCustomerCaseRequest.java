package robert_neat.banking_backend.cases;

/**
 * F8 — wizard submits subject/customer/owner/priority/caseType only; statutoryDeadline and
 * slaRemainingHours are computed by the backend (see CaseActionsService.STATUTORY_DEADLINE_DAYS).
 * ownerId is optional — defaults to the JWT caller when absent.
 */
public record CreateCustomerCaseRequest(
        String subject,
        String customerId,
        String ownerId,
        String priority,
        String caseType) {
}
