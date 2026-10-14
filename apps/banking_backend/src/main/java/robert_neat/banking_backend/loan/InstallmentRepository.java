package robert_neat.banking_backend.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByLoanApplicationIdOrderByInstallmentNoAsc(String loanApplicationId);
}
