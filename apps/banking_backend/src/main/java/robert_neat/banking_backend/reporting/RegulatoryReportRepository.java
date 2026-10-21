package robert_neat.banking_backend.reporting;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegulatoryReportRepository extends JpaRepository<RegulatoryReport, String> {

  List<RegulatoryReport> findByStatusNotOrderByDueDateAsc(String status, Pageable pageable);
}
