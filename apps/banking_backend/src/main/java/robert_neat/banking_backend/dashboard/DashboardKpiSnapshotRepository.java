package robert_neat.banking_backend.dashboard;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardKpiSnapshotRepository extends JpaRepository<DashboardKpiSnapshot, Long> {

  Optional<DashboardKpiSnapshot> findTopByOrderBySnapshotDateDesc();
}
