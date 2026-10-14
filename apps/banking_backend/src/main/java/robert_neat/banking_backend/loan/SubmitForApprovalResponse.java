package robert_neat.banking_backend.loan;

/** POST /loans/{id}/submit-for-approval response — loan detail plus the governance approval id so
 * the frontend can link straight to the approvals queue. */
public record SubmitForApprovalResponse(LoanDetailDto loan, String approvalId) {
}
