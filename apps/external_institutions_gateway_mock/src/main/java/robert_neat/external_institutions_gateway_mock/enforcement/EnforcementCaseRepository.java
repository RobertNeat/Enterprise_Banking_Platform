package robert_neat.external_institutions_gateway_mock.enforcement;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EnforcementCaseRepository extends JpaRepository<EnforcementCase, String> {

    List<EnforcementCase> findAllByOrderByCreatedAtAsc();

    /** Pessimistic write lock so concurrent transfer() calls on the same case can't both pass the status check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EnforcementCase e where e.id = :id")
    Optional<EnforcementCase> findByIdForUpdate(String id);
}
