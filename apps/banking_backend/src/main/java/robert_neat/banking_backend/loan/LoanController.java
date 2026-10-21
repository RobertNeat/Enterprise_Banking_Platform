package robert_neat.banking_backend.loan;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanActionsService loanActionsService;

    public LoanController(LoanService loanService, LoanActionsService loanActionsService) {
        this.loanService = loanService;
        this.loanActionsService = loanActionsService;
    }

    @GetMapping
    public List<LoanSummaryDto> findAll() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public LoanDetailDto findById(@PathVariable String id) {
        return loanService.findById(id);
    }

    /** F2 — submits a new application; scoreResult/dsti come from credit_risk_mock, not the caller. */
    @PostMapping
    public LoanDetailDto create(@RequestBody CreateLoanApplicationRequest request) {
        return loanActionsService.create(request);
    }

    /** F2 — application -> decision: opens a governance_mock maker-checker approval. */
    @PostMapping("/{id}/submit-for-approval")
    public SubmitForApprovalResponse submitForApproval(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return loanActionsService.submitForApproval(id, jwt.getSubject());
    }

    /**
     * F2 — decision -> contract: requires the linked approval to be APPROVED; generates the
     * schedule. A REJECTED approval instead moves the loan back to 'application' so it can be
     * resubmitted (see LoanActionsService.advanceStage).
     */
    @PostMapping("/{id}/advance-stage")
    public LoanDetailDto advanceStage(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return loanActionsService.advanceStage(id, jwt.getSubject());
    }

    /** F2 — contract -> disbursement -> schedule: operational payout step after the contract stage. */
    @PostMapping("/{id}/disburse")
    public LoanDetailDto disburse(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return loanActionsService.disburse(id, jwt.getSubject());
    }
}
