package robert_neat.banking_backend.card;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.InvalidRequestException;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;

/**
 * F4 mutating flow — card block/unblock and chargeback filing/advancement, all proxied through
 * card_processor_mock (see CardProcessorClient). State preconditions (card must be 'active' to
 * block, 'blocked' to unblock) are validated here BEFORE the external call, matching the
 * LoanActionsService pattern: an outage/rejected request must leave no DB row changed and no mock
 * side effect performed on an entity in the wrong state.
 *
 * <p>ICT incident on processor unavailability (F4 alt-flow, see
 * docs/banking_web_functions(concise).md): any CardProcessorUnavailableException caught here is
 * reported to governance_mock as a HIGH severity ICT incident before the 503 is propagated to the
 * caller. If the governance_mock call itself fails, that secondary failure is logged and swallowed
 * — the original card_processor error is what the caller must see, not a masked governance error.
 * This "unavailable -> ICT incident -> rethrow original" pattern is reusable in R4 (F6/F7) wherever
 * a mock's unavailability should be made visible to the DORA ICT incident registry.
 */
@Service
@Transactional
public class CardActionsService {

  private static final Logger log = LoggerFactory.getLogger(CardActionsService.class);

  /**
   * Must mirror the CHECK constraint on chargeback_ref.reason_code
   * (db/changelog/schema/003-card.sql).
   */
  private static final List<String> VALID_REASON_CODES = List.of("4855", "4837", "4853", "4863");

  private final PaymentCardRepository paymentCards;
  private final CardAuthorizationRefRepository authorizations;
  private final ChargebackRefRepository chargebacks;
  private final CardProcessorClient cardProcessorClient;
  private final GovernanceClient governanceClient;

  public CardActionsService(
      PaymentCardRepository paymentCards,
      CardAuthorizationRefRepository authorizations,
      ChargebackRefRepository chargebacks,
      CardProcessorClient cardProcessorClient,
      GovernanceClient governanceClient) {
    this.paymentCards = paymentCards;
    this.authorizations = authorizations;
    this.chargebacks = chargebacks;
    this.cardProcessorClient = cardProcessorClient;
    this.governanceClient = governanceClient;
  }

  public PaymentCardDto blockCard(String cardId, String employeeId) {
    PaymentCard card = requireCard(cardId);
    if (!"active".equals(card.getStatus())) {
      throw new InvalidStageTransitionException(
          "Card " + cardId + " is not 'active' (current: " + card.getStatus() + ")");
    }

    callProcessor("block", cardId, () -> cardProcessorClient.blockCard(cardId));

    card.setStatus("blocked");
    paymentCards.save(card);
    governanceClient.recordAuditEvent(employeeId, "CARD_BLOCKED", "PAYMENT_CARD", cardId, null);

    return CardService.toDto(card);
  }

  public PaymentCardDto unblockCard(String cardId, String employeeId) {
    PaymentCard card = requireCard(cardId);
    if (!"blocked".equals(card.getStatus())) {
      throw new InvalidStageTransitionException(
          "Card " + cardId + " is not 'blocked' (current: " + card.getStatus() + ")");
    }

    callProcessor("unblock", cardId, () -> cardProcessorClient.unblockCard(cardId));

    card.setStatus("active");
    paymentCards.save(card);
    governanceClient.recordAuditEvent(employeeId, "CARD_UNBLOCKED", "PAYMENT_CARD", cardId, null);

    return CardService.toDto(card);
  }

  /**
   * reasonCode is validated before the card_processor_mock call (and before any DB write) —
   * matching LoanActionsService.validateCreateRequest(): an invalid value would otherwise still
   * burn a network call to card_processor_mock and/or surface as a raw
   * DataIntegrityViolationException (500) when chargeback_ref's CHECK constraint rejects the
   * insert.
   */
  public ChargebackDto fileChargeback(
      String cardId, FileChargebackRequest request, String employeeId) {
    if (request.reasonCode() == null || !VALID_REASON_CODES.contains(request.reasonCode())) {
      throw new InvalidRequestException(
          "reasonCode must be one of: " + String.join(", ", VALID_REASON_CODES));
    }

    requireCard(cardId);
    CardAuthorizationRef authorizationRef =
        authorizations
            .findById(request.authorizationId())
            .orElseThrow(
                () ->
                    new NotFoundException("Authorization not found: " + request.authorizationId()));
    if (!authorizationRef.getCard().getId().equals(cardId)) {
      throw new NotFoundException(
          "Authorization " + request.authorizationId() + " does not belong to card " + cardId);
    }

    CardProcessorChargebackResponse response =
        callProcessor(
            "fileChargeback",
            cardId,
            () ->
                cardProcessorClient.fileChargeback(
                    request.authorizationId(), request.reasonCode(), request.description()));

    ChargebackRef chargeback = new ChargebackRef();
    chargeback.setId(response.id());
    chargeback.setCardAuthorizationRef(authorizationRef);
    chargeback.setReasonCode(response.reasonCode());
    chargeback.setStatus(response.status());
    chargeback.setFiledAt(response.filedAt() != null ? response.filedAt() : Instant.now());
    chargebacks.save(chargeback);

    governanceClient.recordAuditEvent(
        employeeId, "CARD_CHARGEBACK_FILED", "CHARGEBACK", chargeback.getId(), null);

    return CardService.toDto(chargeback);
  }

  public ChargebackDto advanceChargeback(
      String chargebackId, AdvanceChargebackRequest request, String employeeId) {
    ChargebackRef chargeback =
        chargebacks
            .findById(chargebackId)
            .orElseThrow(() -> new NotFoundException("Chargeback not found: " + chargebackId));
    String cardId = chargeback.getCardAuthorizationRef().getCard().getId();

    CardProcessorChargebackResponse response =
        callProcessor(
            "advanceChargeback",
            cardId,
            () -> cardProcessorClient.advanceChargeback(chargebackId, request.status()));

    chargeback.setStatus(response.status());
    chargebacks.save(chargeback);

    governanceClient.recordAuditEvent(
        employeeId, "CARD_CHARGEBACK_ADVANCED", "CHARGEBACK", chargebackId, request.status());

    return CardService.toDto(chargeback);
  }

  private <T> T callProcessor(
      String operation, String cardId, java.util.function.Supplier<T> call) {
    try {
      return call.get();
    } catch (CardProcessorUnavailableException ex) {
      reportIctIncident(operation, cardId, ex);
      throw ex;
    }
  }

  private void reportIctIncident(
      String operation, String cardId, CardProcessorUnavailableException cause) {
    try {
      governanceClient.createIctIncident(
          "CARD_PROCESSOR",
          "card_processor_mock unavailable during "
              + operation
              + " for card "
              + cardId
              + ": "
              + cause.getMessage(),
          "HIGH");
    } catch (RuntimeException ictFailure) {
      // Double failure (governance_mock also unavailable) — do not mask the original
      // card_processor error with this secondary one. Log and continue propagating `cause`.
      log.warn(
          "Failed to record ICT incident for card_processor outage ({}): {}",
          operation,
          ictFailure.getMessage());
    }
  }

  private PaymentCard requireCard(String cardId) {
    return paymentCards
        .findById(cardId)
        .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
  }
}
