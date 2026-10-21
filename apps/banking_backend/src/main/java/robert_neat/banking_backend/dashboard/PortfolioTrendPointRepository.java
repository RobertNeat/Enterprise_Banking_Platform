package robert_neat.banking_backend.dashboard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioTrendPointRepository extends JpaRepository<PortfolioTrendPoint, Long> {

  List<PortfolioTrendPoint> findByOrderBySnapshotDateAsc();
}
