package robert_neat.governance_mock.incident;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface IctIncidentRepository extends JpaRepository<IctIncident, String> {

    List<IctIncident> findAllByOrderByCreatedAtAsc();

    List<IctIncident> findByStatusOrderByCreatedAtAsc(IncidentStatus status);

    List<IctIncident> findBySeverityOrderByCreatedAtAsc(Severity severity);

    List<IctIncident> findByStatusAndSeverityOrderByCreatedAtAsc(IncidentStatus status, Severity severity);

    /** Pessimistic write lock so concurrent resolve() calls can't both pass the OPEN check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IctIncident i where i.id = :id")
    Optional<IctIncident> findByIdForUpdate(String id);
}
