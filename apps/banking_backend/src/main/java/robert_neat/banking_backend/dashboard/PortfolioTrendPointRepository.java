package robert_neat.banking_backend.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioTrendPointRepository extends JpaRepository<PortfolioTrendPoint, Long> {

    List<PortfolioTrendPoint> findByOrderBySnapshotDateAsc();
}
