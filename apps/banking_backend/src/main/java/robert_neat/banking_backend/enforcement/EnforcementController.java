package robert_neat.banking_backend.enforcement;

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
@RequestMapping("/enforcement")
public class EnforcementController {

  private final EnforcementService enforcementService;
  private final EnforcementActionsService enforcementActionsService;

  public EnforcementController(
      EnforcementService enforcementService, EnforcementActionsService enforcementActionsService) {
    this.enforcementService = enforcementService;
    this.enforcementActionsService = enforcementActionsService;
  }

  @GetMapping
  public List<EnforcementCaseDto> findAll() {
    return enforcementService.findAll();
  }

  /** F6 — registers a new enforcement case via external_institutions_gateway_mock. */
  @PostMapping
  public EnforcementCaseDto register(
      @RequestBody RegisterEnforcementRequest request, @AuthenticationPrincipal Jwt jwt) {
    return enforcementActionsService.register(request, jwt.getSubject());
  }

  /**
   * F6 — opens a governance_mock maker-checker approval for the funds transfer; registered ->
   * active.
   */
  @PostMapping("/{id}/request-transfer-approval")
  public RequestTransferApprovalResponse requestTransferApproval(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return enforcementActionsService.requestTransferApproval(id, jwt.getSubject());
  }

  /** F6 — transfers blocked funds to the bailiff; requires the linked approval to be APPROVED. */
  @PostMapping("/{id}/transfer")
  public EnforcementCaseDto transfer(
      @PathVariable String id,
      @RequestBody TransferEnforcementRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return enforcementActionsService.transfer(id, request, jwt.getSubject());
  }
}
