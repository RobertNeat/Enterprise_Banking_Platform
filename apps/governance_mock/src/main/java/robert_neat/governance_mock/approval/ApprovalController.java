package robert_neat.governance_mock.approval;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    public ResponseEntity<ApprovalRequestRecord> create(@RequestBody CreateApprovalRequest request) {
        ApprovalRequestRecord created = approvalService.create(request.type(), request.requestedBy(), request.payload());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ApprovalRequestRecord> list(@RequestParam(required = false) ApprovalStatus status) {
        return approvalService.list(status);
    }

    @GetMapping("/{id}")
    public ApprovalRequestRecord get(@PathVariable String id) {
        return approvalService.get(id);
    }

    @PostMapping("/{id}/decide")
    public ApprovalRequestRecord decide(@PathVariable String id, @RequestBody DecideRequest request) {
        return approvalService.decide(id, request.decision(), request.decidedBy());
    }
}
