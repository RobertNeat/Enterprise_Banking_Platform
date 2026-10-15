package robert_neat.banking_backend.card;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;
    private final CardActionsService cardActionsService;

    public CardController(CardService cardService, CardActionsService cardActionsService) {
        this.cardService = cardService;
        this.cardActionsService = cardActionsService;
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

    /** F4 — blocks the card via card_processor_mock; 409 if not currently 'active'. */
    @PostMapping("/{cardId}/block")
    public PaymentCardDto block(@PathVariable String cardId, @AuthenticationPrincipal Jwt jwt) {
        return cardActionsService.blockCard(cardId, jwt.getSubject());
    }

    /** F4 — unblocks the card via card_processor_mock; 409 if not currently 'blocked'. */
    @PostMapping("/{cardId}/unblock")
    public PaymentCardDto unblock(@PathVariable String cardId, @AuthenticationPrincipal Jwt jwt) {
        return cardActionsService.unblockCard(cardId, jwt.getSubject());
    }

    /** F4 — files a chargeback against one of this card's authorizations. */
    @PostMapping("/{cardId}/chargebacks")
    public ChargebackDto fileChargeback(
            @PathVariable String cardId,
            @RequestBody FileChargebackRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return cardActionsService.fileChargeback(cardId, request, jwt.getSubject());
    }

    /** F4 — advances a chargeback through its lifecycle (FILED -> UNDER_REVIEW -> RESOLVED_*). */
    @PostMapping("/chargebacks/{id}/advance")
    public ChargebackDto advanceChargeback(
            @PathVariable String id,
            @RequestBody AdvanceChargebackRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return cardActionsService.advanceChargeback(id, request, jwt.getSubject());
    }
}
