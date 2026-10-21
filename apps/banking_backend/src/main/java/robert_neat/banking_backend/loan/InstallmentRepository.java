package robert_neat.banking_backend.loan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

  List<Installment> findByLoanApplicationIdOrderByInstallmentNoAsc(String loanApplicationId);
}
