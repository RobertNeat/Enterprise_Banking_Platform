package robert_neat.governance_mock.approval;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ApprovalRepository extends JpaRepository<ApprovalRequestRecord, String> {

    List<ApprovalRequestRecord> findByStatusOrderByCreatedAtAsc(ApprovalStatus status);

    List<ApprovalRequestRecord> findAllByOrderByCreatedAtAsc();

    /** Pessimistic write lock so concurrent decide() calls can't both pass the PENDING check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ApprovalRequestRecord a where a.id = :id")
    Optional<ApprovalRequestRecord> findByIdForUpdate(String id);
}
