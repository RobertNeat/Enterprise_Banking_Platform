package robert_neat.banking_backend.collections;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionsCaseRepository extends JpaRepository<CollectionsCase, String> {

    List<CollectionsCase> findByLoanApplicationId(String loanApplicationId);
}
