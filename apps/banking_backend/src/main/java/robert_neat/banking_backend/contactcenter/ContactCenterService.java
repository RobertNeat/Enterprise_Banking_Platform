package robert_neat.banking_backend.contactcenter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.card.PaymentCardRepository;
import robert_neat.banking_backend.cases.CustomerCaseRepository;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.customer.Customer;
import robert_neat.banking_backend.customer.CustomerRepository;
import robert_neat.banking_backend.loan.LoanApplicationRepository;

import java.util.List;

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

    public List<ServiceDispositionDto> findAllDispositions() {
        return dispositions.findAll().stream()
                .map(d -> new ServiceDispositionDto(
                        d.getId(),
                        d.getTypeKey(),
                        d.getStatus(),
                        d.getCustomerCase().getId(),
                        d.getOccurredAt()))
                .toList();
    }

    public List<ContactHistoryEntryDto> findAllHistory() {
        return history.findAll().stream()
                .map(h -> new ContactHistoryEntryDto(h.getId(), h.getChannelKey(), h.getOccurredAt(), h.getSummary()))
                .toList();
    }

    public CustomerProfileDto findProfile(String customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        long loansCount = loanApplications.findByCustomerId(customerId).size();
        long cardsCount = paymentCards.findByCustomerId(customerId).size();
        long openCases = customerCases.findByCustomerId(customerId).stream()
                .filter(c -> !"resolved".equals(c.getStatus()))
                .count();
        return new CustomerProfileDto(
                customer.getFullName(), customer.getSegment(), loansCount, cardsCount,
                customer.getRiskRating(), openCases);
    }
}
