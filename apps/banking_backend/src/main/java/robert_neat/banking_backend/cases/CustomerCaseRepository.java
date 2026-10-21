package robert_neat.banking_backend.cases;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCaseRepository extends JpaRepository<CustomerCase, String> {

  List<CustomerCase> findByCustomerId(String customerId);

  List<CustomerCase> findByOrderByRegisteredAtDesc(Pageable pageable);

  /** F8 — CaseSlaEscalationScheduler's overdue-case sweep; status filter is the dedup guard. */
  List<CustomerCase> findBySlaDueAtBeforeAndStatusIn(Instant slaDueAt, Collection<String> statuses);
}
