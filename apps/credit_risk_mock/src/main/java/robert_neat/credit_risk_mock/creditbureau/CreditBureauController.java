package robert_neat.credit_risk_mock.creditbureau;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credit-bureau")
public class CreditBureauController {

    private final CreditBureauService creditBureauService;

    public CreditBureauController(CreditBureauService creditBureauService) {
        this.creditBureauService = creditBureauService;
    }

    @PostMapping("/score")
    public ResponseEntity<CreditBureauInquiry> score(@RequestBody ScoreRequest request) {
        return ResponseEntity.ok(creditBureauService.score(request));
    }

    @GetMapping("/history/{applicantId}")
    public ResponseEntity<List<CreditBureauInquiry>> history(@PathVariable String applicantId) {
        return ResponseEntity.ok(creditBureauService.history(applicantId));
    }
}
