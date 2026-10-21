package robert_neat.banking_backend.contactcenter;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactHistoryEntryRepository extends JpaRepository<ContactHistoryEntry, String> {

  List<ContactHistoryEntry> findByCustomer_Id(String customerId);
}
