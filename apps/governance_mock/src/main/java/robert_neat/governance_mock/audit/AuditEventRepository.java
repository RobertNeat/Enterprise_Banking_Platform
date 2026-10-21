package robert_neat.governance_mock.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    List<AuditEvent> findAllByOrderByTimestampAsc();

    // entityType is matched case-insensitively (see AuditService), entityId case-sensitively.
    List<AuditEvent> findByEntityTypeIgnoreCaseOrderByTimestampAsc(String entityType);

    List<AuditEvent> findByEntityIdOrderByTimestampAsc(String entityId);

    List<AuditEvent> findByEntityTypeIgnoreCaseAndEntityIdOrderByTimestampAsc(String entityType, String entityId);
}
