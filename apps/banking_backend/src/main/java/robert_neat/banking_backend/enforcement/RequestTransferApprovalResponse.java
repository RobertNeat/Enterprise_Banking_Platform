package robert_neat.banking_backend.enforcement;

public record RequestTransferApprovalResponse(
    EnforcementCaseDto enforcementCase, String approvalId) {}
