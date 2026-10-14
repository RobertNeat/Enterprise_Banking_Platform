package robert_neat.credit_risk_mock.ecl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ecl")
public class EclController {

    private final EclEngineService eclEngineService;

    public EclController(EclEngineService eclEngineService) {
        this.eclEngineService = eclEngineService;
    }

    @PostMapping("/classify")
    public ResponseEntity<EclClassification> classify(@RequestBody ClassifyRequest request) {
        return ResponseEntity.ok(eclEngineService.classify(request));
    }

    @GetMapping("/positions/{positionId}")
    public ResponseEntity<EclClassification> latest(@PathVariable String positionId) {
        return ResponseEntity.ok(eclEngineService.latest(positionId));
    }
}
