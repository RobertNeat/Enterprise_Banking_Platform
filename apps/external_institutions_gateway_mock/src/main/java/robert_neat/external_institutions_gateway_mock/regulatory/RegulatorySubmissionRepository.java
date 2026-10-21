package robert_neat.external_institutions_gateway_mock.regulatory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RegulatorySubmissionRepository extends JpaRepository<RegulatorySubmission, String> {

    List<RegulatorySubmission> findAllByOrderBySubmittedAtAsc();

    List<RegulatorySubmission> findByStatus(SubmissionStatus status);

    /** Pessimistic write lock so concurrent resolve() calls on the same submission can't both apply a resolution. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RegulatorySubmission s where s.submissionId = :submissionId")
    Optional<RegulatorySubmission> findByIdForUpdate(String submissionId);
}
