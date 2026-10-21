package robert_neat.banking_backend.customer;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.common.NotFoundException;

@Service
@Transactional(readOnly = true)
public class CustomerService {

  private final CustomerRepository customers;
  private final EmployeeRepository employees;
  private final BankAccountRepository bankAccounts;

  public CustomerService(
      CustomerRepository customers,
      EmployeeRepository employees,
      BankAccountRepository bankAccounts) {
    this.customers = customers;
    this.employees = employees;
    this.bankAccounts = bankAccounts;
  }

  public List<CustomerDto> findAllCustomers() {
    return customers.findAll().stream()
        .map(
            c ->
                new CustomerDto(
                    c.getId(), c.getFullName(), c.getSegment(), c.getTaxId(), c.getRiskRating()))
        .toList();
  }

  public List<EmployeeDto> findAllEmployees() {
    return employees.findAll().stream().map(CustomerService::toEmployeeDto).toList();
  }

  public List<BankAccountDto> findAccountsByCustomerId(String customerId) {
    if (!customers.existsById(customerId)) {
      throw new NotFoundException("Customer not found: " + customerId);
    }
    return bankAccounts.findByCustomerId(customerId).stream()
        .map(
            a ->
                new BankAccountDto(
                    a.getId(), a.getMaskedNumber(), a.getBalance(), a.getBlockedAmount()))
        .toList();
  }

  private static EmployeeDto toEmployeeDto(Employee e) {
    List<String> businessLines =
        e.getBusinessLines() == null || e.getBusinessLines().isBlank()
            ? List.of()
            : Arrays.stream(e.getBusinessLines().split(",")).map(String::trim).toList();
    return new EmployeeDto(
        e.getId(),
        e.getFirstName(),
        e.getLastName(),
        e.getEmail(),
        e.getPhone(),
        e.getDepartment(),
        e.getBranch(),
        e.getPosition(),
        e.getSupervisor() == null ? null : e.getSupervisor().getId(),
        e.getRole(),
        businessLines,
        e.getStartDate(),
        e.getStatus());
  }
}
