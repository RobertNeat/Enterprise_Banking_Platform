package robert_neat.banking_backend.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {

  List<BankAccount> findByCustomerId(String customerId);
}
