package robert_neat.banking_backend.contactcenter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactHistoryEntryRepository extends JpaRepository<ContactHistoryEntry, String> {
}
