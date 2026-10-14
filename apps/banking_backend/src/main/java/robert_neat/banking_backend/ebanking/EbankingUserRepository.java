package robert_neat.banking_backend.ebanking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EbankingUserRepository extends JpaRepository<EbankingUser, String> {
}
