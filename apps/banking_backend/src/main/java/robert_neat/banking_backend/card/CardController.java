package robert_neat.banking_backend.card;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public List<PaymentCardDto> findAllCards() {
        return cardService.findAllCards();
    }

    @GetMapping("/authorizations")
    public List<CardAuthorizationDto> findAllAuthorizations() {
        return cardService.findAllAuthorizations();
    }

    @GetMapping("/chargebacks")
    public List<ChargebackDto> findAllChargebacks() {
        return cardService.findAllChargebacks();
    }
}
