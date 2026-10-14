package robert_neat.banking_backend.cases;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerCaseRepository extends JpaRepository<CustomerCase, String> {

    List<CustomerCase> findByCustomerId(String customerId);
}
