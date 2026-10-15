package robert_neat.banking_backend.card;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.common.BankTimeZone;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CardService {

    private final PaymentCardRepository paymentCards;
    private final CardAuthorizationRefRepository authorizations;
    private final ChargebackRefRepository chargebacks;

    public CardService(
            PaymentCardRepository paymentCards,
            CardAuthorizationRefRepository authorizations,
            ChargebackRefRepository chargebacks) {
        this.paymentCards = paymentCards;
        this.authorizations = authorizations;
        this.chargebacks = chargebacks;
    }

    public List<PaymentCardDto> findAllCards() {
        return paymentCards.findAll().stream().map(CardService::toDto).toList();
    }

    public static PaymentCardDto toDto(PaymentCard card) {
        return new PaymentCardDto(
                card.getId(),
                card.getCustomer().getFullName(),
                card.getMaskedNumber(),
                card.getNetwork(),
                card.getProduct(),
                card.getStatus(),
                card.getExpiry(),
                card.getLimitDaily(),
                card.getLimitAtm());
    }

    public List<CardAuthorizationDto> findAllAuthorizations() {
        return authorizations.findAll().stream()
                .map(auth -> new CardAuthorizationDto(
                        auth.getId(),
                        auth.getCard().getId(),
                        auth.getMerchant(),
                        auth.getAmount(),
                        auth.getOccurredAt().atZone(BankTimeZone.ZONE).toLocalDate(),
                        auth.getProcessor(),
                        Boolean.TRUE.equals(auth.getApproved())))
                .toList();
    }

    public List<ChargebackDto> findAllChargebacks() {
        return chargebacks.findAll().stream().map(CardService::toDto).toList();
    }

    public static ChargebackDto toDto(ChargebackRef cb) {
        return new ChargebackDto(
                cb.getId(),
                cb.getCardAuthorizationRef().getId(),
                cb.getReasonCode(),
                cb.getStatus(),
                cb.getFiledAt());
    }
}
