package robert_neat.banking_backend.loan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanStageTransitionRepository extends JpaRepository<LoanStageTransition, Long> {
}
