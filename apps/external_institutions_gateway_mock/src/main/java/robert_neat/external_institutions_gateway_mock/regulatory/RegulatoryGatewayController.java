package robert_neat.external_institutions_gateway_mock.regulatory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/regulatory-gateway")
public class RegulatoryGatewayController {

    private final RegulatoryGatewayService regulatoryGatewayService;

    public RegulatoryGatewayController(RegulatoryGatewayService regulatoryGatewayService) {
        this.regulatoryGatewayService = regulatoryGatewayService;
    }

    @PostMapping("/submit")
    public ResponseEntity<RegulatorySubmission> submit(@RequestBody RegulatorySubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(regulatoryGatewayService.submit(request));
    }

    @GetMapping("/submissions/{submissionId}")
    public RegulatorySubmission get(@PathVariable String submissionId) {
        return regulatoryGatewayService.get(submissionId);
    }
}
