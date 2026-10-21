package robert_neat.banking_backend.reporting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportSubmissionRepository extends JpaRepository<ReportSubmission, Long> {

  Optional<ReportSubmission> findFirstByRegulatoryReportIdOrderBySubmittedAtDesc(
      String regulatoryReportId);
}
