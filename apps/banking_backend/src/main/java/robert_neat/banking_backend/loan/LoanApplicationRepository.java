package robert_neat.banking_backend.loan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

  List<LoanApplication> findByCustomerId(String customerId);
}
