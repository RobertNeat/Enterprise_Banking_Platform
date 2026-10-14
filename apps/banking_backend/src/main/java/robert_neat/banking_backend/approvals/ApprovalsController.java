package robert_neat.banking_backend.approvals;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Proxy to governance_mock's approval queue (maker-checker) — see docs/governance_mock_contract/api.md. */
@RestController
@RequestMapping("/approvals")
public class ApprovalsController {

    private final ApprovalsService approvalsService;

    public ApprovalsController(ApprovalsService approvalsService) {
        this.approvalsService = approvalsService;
    }

    @GetMapping
    public List<ApprovalRecord> list(@RequestParam(required = false) String status) {
        return approvalsService.list(status);
    }

    @GetMapping("/{id}")
    public ApprovalRecord get(@PathVariable String id) {
        return approvalsService.get(id);
    }

    @PostMapping("/{id}/decide")
    public ApprovalRecord decide(
            @PathVariable String id,
            @RequestBody DecideApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String checkerEmployeeId = jwt.getSubject();
        int checkerSeniorityLevel = ((Number) jwt.getClaim("seniorityLevel")).intValue();
        return approvalsService.decide(id, request.decision(), checkerEmployeeId, checkerSeniorityLevel);
    }
}
