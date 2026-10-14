package robert_neat.banking_backend.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardKpiSnapshotRepository extends JpaRepository<DashboardKpiSnapshot, Long> {

    Optional<DashboardKpiSnapshot> findTopByOrderBySnapshotDateDesc();
}
