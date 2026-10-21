package robert_neat.banking_backend.loan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanStageTransitionRepository extends JpaRepository<LoanStageTransition, Long> {

  List<LoanStageTransition> findByLoanApplicationIdOrderByTransitionedAtAsc(
      String loanApplicationId);
}
