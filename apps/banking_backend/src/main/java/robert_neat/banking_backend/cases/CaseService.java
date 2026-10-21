package robert_neat.banking_backend.cases;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.common.EmployeeNames;

@Service
@Transactional(readOnly = true)
public class CaseService {

  private final CustomerCaseRepository customerCases;

  public CaseService(CustomerCaseRepository customerCases) {
    this.customerCases = customerCases;
  }

  public List<CustomerCaseDto> findAll() {
    Instant now = Instant.now();
    return customerCases.findAll().stream().map(c -> toDto(c, now)).toList();
  }

  static CustomerCaseDto toDto(CustomerCase customerCase, Instant now) {
    long hoursRemaining = Math.max(0, Duration.between(now, customerCase.getSlaDueAt()).toHours());
    return new CustomerCaseDto(
        customerCase.getId(),
        customerCase.getSubject(),
        customerCase.getCustomer().getFullName(),
        EmployeeNames.abbreviated(customerCase.getOwner()),
        customerCase.getPriority(),
        customerCase.getStatutoryDeadline(),
        hoursRemaining,
        customerCase.getStatus());
  }
}
