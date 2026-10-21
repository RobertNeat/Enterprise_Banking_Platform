package robert_neat.banking_backend.contactcenter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactHistoryEntryRepository extends JpaRepository<ContactHistoryEntry, String> {

    List<ContactHistoryEntry> findByCustomer_Id(String customerId);
}
