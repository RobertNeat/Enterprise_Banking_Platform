package robert_neat.banking_backend.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, String> {

    List<PaymentCard> findByCustomerId(String customerId);
}
