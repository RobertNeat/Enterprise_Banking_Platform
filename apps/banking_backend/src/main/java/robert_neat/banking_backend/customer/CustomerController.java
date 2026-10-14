package robert_neat.banking_backend.customer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    public List<CustomerDto> findAllCustomers() {
        return customerService.findAllCustomers();
    }

    @GetMapping("/employees")
    public List<EmployeeDto> findAllEmployees() {
        return customerService.findAllEmployees();
    }

    @GetMapping("/customers/{id}/accounts")
    public List<BankAccountDto> findAccounts(@PathVariable String id) {
        return customerService.findAccountsByCustomerId(id);
    }
}
