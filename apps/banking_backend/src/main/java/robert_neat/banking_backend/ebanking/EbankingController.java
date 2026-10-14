package robert_neat.banking_backend.ebanking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ebanking")
public class EbankingController {

    private final EbankingService ebankingService;

    public EbankingController(EbankingService ebankingService) {
        this.ebankingService = ebankingService;
    }

    @GetMapping("/users")
    public List<EbankingUserDto> findAll() {
        return ebankingService.findAll();
    }
}
