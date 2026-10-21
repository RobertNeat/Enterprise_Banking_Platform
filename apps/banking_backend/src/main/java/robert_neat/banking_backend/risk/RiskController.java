package robert_neat.banking_backend.risk;

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
@RequestMapping("/risk")
public class RiskController {

  private final RiskService riskService;
  private final RiskActionsService riskActionsService;

  public RiskController(RiskService riskService, RiskActionsService riskActionsService) {
    this.riskService = riskService;
    this.riskActionsService = riskActionsService;
  }

  @GetMapping("/collateral")
  public List<CollateralEntryDto> findAll() {
    return riskService.findAll();
  }

  /** F3 — reclassifies a collateral position's IFRS 9 stage/provision via credit_risk_mock. */
  @PostMapping("/collateral/{id}/reclassify")
  public CollateralEntryDto reclassify(
      @PathVariable String id,
      @RequestBody ReclassifyCollateralRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return riskActionsService.reclassify(id, request.daysPastDue(), jwt.getSubject());
  }
}
