package robert_neat.external_institutions_gateway_mock.enforcement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enforcement")
public class EnforcementController {

    private final EnforcementService enforcementService;

    public EnforcementController(EnforcementService enforcementService) {
        this.enforcementService = enforcementService;
    }

    @PostMapping("/incoming")
    public ResponseEntity<EnforcementCase> incoming(@RequestBody IncomingEnforcementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enforcementService.registerIncoming(request));
    }

    @GetMapping
    public List<EnforcementCase> list() {
        return enforcementService.list();
    }

    @PostMapping("/{id}/transfer")
    public SettlementConfirmation transfer(@PathVariable String id, @RequestBody TransferRequest request) {
        return enforcementService.transfer(id, request.transferredAmount());
    }
}
