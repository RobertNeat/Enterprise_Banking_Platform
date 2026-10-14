package robert_neat.banking_backend.enforcement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnforcementCaseRepository extends JpaRepository<EnforcementCase, String> {
}
