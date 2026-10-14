package robert_neat.banking_backend.enforcement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/enforcement")
public class EnforcementController {

    private final EnforcementService enforcementService;

    public EnforcementController(EnforcementService enforcementService) {
        this.enforcementService = enforcementService;
    }

    @GetMapping
    public List<EnforcementCaseDto> findAll() {
        return enforcementService.findAll();
    }
}
