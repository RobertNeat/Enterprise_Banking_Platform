package robert_neat.banking_backend.cases;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cases")
public class CaseController {

  private final CaseService caseService;
  private final CaseActionsService caseActionsService;

  public CaseController(CaseService caseService, CaseActionsService caseActionsService) {
    this.caseService = caseService;
    this.caseActionsService = caseActionsService;
  }

  @GetMapping
  public List<CustomerCaseDto> findAll() {
    return caseService.findAll();
  }

  /**
   * F8 — statutoryDeadline/slaRemainingHours are computed by the backend, not accepted from the
   * caller.
   */
  @PostMapping
  public CustomerCaseDto create(
      @RequestBody CreateCustomerCaseRequest request, @AuthenticationPrincipal Jwt jwt) {
    return caseActionsService.create(request, jwt.getSubject());
  }

  /** F8 — evaluates SLA via governance_mock; escalates and audits (auto or manual) accordingly. */
  @PostMapping("/{id}/escalate")
  public EscalateCaseResponse escalate(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return caseActionsService.escalate(id, jwt.getSubject());
  }

  /** CAS lifecycle — only 'new' -> 'inProgress'; 409 otherwise. */
  @PostMapping("/{id}/start")
  public CustomerCaseDto start(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return caseActionsService.start(id, jwt.getSubject());
  }

  /** CAS lifecycle — 'new'/'inProgress'/'escalated' -> 'resolved'; 409 if already resolved. */
  @PostMapping("/{id}/resolve")
  public CustomerCaseDto resolve(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return caseActionsService.resolve(id, jwt.getSubject());
  }
}
