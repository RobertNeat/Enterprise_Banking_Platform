package robert_neat.banking_backend.contactcenter;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.card.PaymentCardRepository;
import robert_neat.banking_backend.cases.CustomerCase;
import robert_neat.banking_backend.cases.CustomerCaseRepository;
import robert_neat.banking_backend.common.InvalidRequestException;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.customer.Customer;
import robert_neat.banking_backend.customer.CustomerRepository;
import robert_neat.banking_backend.loan.LoanApplicationRepository;

@Service
@Transactional(readOnly = true)
public class ContactCenterService {

  private final ServiceDispositionRepository dispositions;
  private final ContactHistoryEntryRepository history;
  private final CustomerRepository customers;
  private final LoanApplicationRepository loanApplications;
  private final PaymentCardRepository paymentCards;
  private final CustomerCaseRepository customerCases;

  public ContactCenterService(
      ServiceDispositionRepository dispositions,
      ContactHistoryEntryRepository history,
      CustomerRepository customers,
      LoanApplicationRepository loanApplications,
      PaymentCardRepository paymentCards,
      CustomerCaseRepository customerCases) {
    this.dispositions = dispositions;
    this.history = history;
    this.customers = customers;
    this.loanApplications = loanApplications;
    this.paymentCards = paymentCards;
    this.customerCases = customerCases;
  }

  /**
   * KKT — customerId optional; dispositions have no direct customer FK, filtered via their case.
   */
  public List<ServiceDispositionDto> findAllDispositions(String customerId) {
    List<ServiceDisposition> rows =
        customerId == null || customerId.isBlank()
            ? dispositions.findAll()
            : dispositions.findByCustomerCase_Customer_Id(customerId);
    return rows.stream()
        .map(
            d ->
                new ServiceDispositionDto(
                    d.getId(),
                    d.getTypeKey(),
                    d.getStatus(),
                    d.getCustomerCase().getId(),
                    d.getOccurredAt()))
        .toList();
  }

  /** KKT — customerId optional. */
  public List<ContactHistoryEntryDto> findAllHistory(String customerId) {
    List<ContactHistoryEntry> rows =
        customerId == null || customerId.isBlank()
            ? history.findAll()
            : history.findByCustomer_Id(customerId);
    return rows.stream()
        .map(
            h ->
                new ContactHistoryEntryDto(
                    h.getId(), h.getChannelKey(), h.getOccurredAt(), h.getSummary()))
        .toList();
  }

  /**
   * KKT/F8 — links a disposition to a case. The target case must belong to the same customer as the
   * disposition's current case: allowing a cross-customer link would reintroduce the exact
   * seed-data bug fixed in seed/007-contactcenter.sql (DSP-771 previously pointed at CS-10231, a
   * different customer's case).
   */
  @Transactional
  public ServiceDispositionDto linkCase(String dispositionId, String caseId) {
    if (caseId == null || caseId.isBlank()) {
      throw new InvalidRequestException("caseId must not be blank");
    }
    ServiceDisposition disposition =
        dispositions
            .findById(dispositionId)
            .orElseThrow(() -> new NotFoundException("Disposition not found: " + dispositionId));
    CustomerCase targetCase =
        customerCases
            .findById(caseId)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseId));

    String currentCustomerId = disposition.getCustomerCase().getCustomer().getId();
    String targetCustomerId = targetCase.getCustomer().getId();
    if (!currentCustomerId.equals(targetCustomerId)) {
      throw new InvalidRequestException(
          "Case " + caseId + " belongs to a different customer than disposition " + dispositionId);
    }

    disposition.setCustomerCase(targetCase);
    dispositions.save(disposition);

    return new ServiceDispositionDto(
        disposition.getId(),
        disposition.getTypeKey(),
        disposition.getStatus(),
        targetCase.getId(),
        disposition.getOccurredAt());
  }

  public CustomerProfileDto findProfile(String customerId) {
    Customer customer =
        customers
            .findById(customerId)
            .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    long loansCount = loanApplications.findByCustomerId(customerId).size();
    long cardsCount = paymentCards.findByCustomerId(customerId).size();
    long openCases =
        customerCases.findByCustomerId(customerId).stream()
            .filter(c -> !"resolved".equals(c.getStatus()))
            .count();
    return new CustomerProfileDto(
        customer.getFullName(),
        customer.getSegment(),
        loansCount,
        cardsCount,
        customer.getRiskRating(),
        openCases);
  }
}
