package robert_neat.banking_backend.collections;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionsCaseRepository extends JpaRepository<CollectionsCase, String> {

  List<CollectionsCase> findByLoanApplicationId(String loanApplicationId);
}
