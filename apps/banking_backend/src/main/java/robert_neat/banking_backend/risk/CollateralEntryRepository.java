package robert_neat.banking_backend.risk;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollateralEntryRepository extends JpaRepository<CollateralEntry, String> {
}
