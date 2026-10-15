package robert_neat.banking_backend.reporting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportSubmissionRepository extends JpaRepository<ReportSubmission, Long> {

    Optional<ReportSubmission> findFirstByRegulatoryReportIdOrderBySubmittedAtDesc(String regulatoryReportId);
}
