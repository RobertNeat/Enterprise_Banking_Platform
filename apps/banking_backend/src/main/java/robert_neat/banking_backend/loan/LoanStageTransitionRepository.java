package robert_neat.banking_backend.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanStageTransitionRepository extends JpaRepository<LoanStageTransition, Long> {

    List<LoanStageTransition> findByLoanApplicationIdOrderByTransitionedAtAsc(String loanApplicationId);
}
