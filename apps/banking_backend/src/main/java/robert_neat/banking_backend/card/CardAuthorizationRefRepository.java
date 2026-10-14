package robert_neat.banking_backend.card;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardAuthorizationRefRepository extends JpaRepository<CardAuthorizationRef, String> {
}
