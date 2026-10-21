package robert_neat.banking_backend.card;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, String> {

  List<PaymentCard> findByCustomerId(String customerId);
}
