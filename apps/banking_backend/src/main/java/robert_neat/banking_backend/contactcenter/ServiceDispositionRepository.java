package robert_neat.banking_backend.contactcenter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceDispositionRepository extends JpaRepository<ServiceDisposition, String> {

    /** Disposition has no direct customer FK — filtered via its linked case's customer. */
    List<ServiceDisposition> findByCustomerCase_Customer_Id(String customerId);
}
