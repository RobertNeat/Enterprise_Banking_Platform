package robert_neat.external_institutions_gateway_mock.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import robert_neat.external_institutions_gateway_mock.enforcement.EnforcementService;
import robert_neat.external_institutions_gateway_mock.enforcement.IncomingEnforcementRequest;
import robert_neat.external_institutions_gateway_mock.fatca.FatcaCrsService;
import robert_neat.external_institutions_gateway_mock.regulatory.RegulatoryGatewayService;

import java.math.BigDecimal;

/**
 * Panel operatorski (Thymeleaf) do recznego testowania -- osobny od REST API ponizej.
 * Formularz HTML wysyla x-www-form-urlencoded, wiec nie reuzywa @RequestBody kontrolerow REST.
 */
@Controller
public class UiController {

    private final EnforcementService enforcementService;
    private final FatcaCrsService fatcaCrsService;
    private final RegulatoryGatewayService regulatoryGatewayService;

    public UiController(EnforcementService enforcementService, FatcaCrsService fatcaCrsService,
                         RegulatoryGatewayService regulatoryGatewayService) {
        this.enforcementService = enforcementService;
        this.fatcaCrsService = fatcaCrsService;
        this.regulatoryGatewayService = regulatoryGatewayService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/ui/enforcement")
    public String enforcement(Model model) {
        model.addAttribute("cases", enforcementService.list());
        return "enforcement/index";
    }

    @PostMapping("/ui/enforcement")
    public String createEnforcement(@RequestParam String bailiff,
                                     @RequestParam String caseNumber,
                                     @RequestParam String customer,
                                     @RequestParam BigDecimal blockedAmount,
                                     @RequestParam(required = false) BigDecimal protectedAmount) {
        enforcementService.registerIncoming(
                new IncomingEnforcementRequest(bailiff, caseNumber, customer, blockedAmount, protectedAmount));
        return "redirect:/ui/enforcement";
    }

    @GetMapping("/ui/reports")
    public String reports(Model model) {
        model.addAttribute("submissions", regulatoryGatewayService.list());
        return "reports/index";
    }

    @GetMapping("/ui/fatca")
    public String fatca(Model model) {
        model.addAttribute("exports", fatcaCrsService.list());
        return "fatca/index";
    }
}
