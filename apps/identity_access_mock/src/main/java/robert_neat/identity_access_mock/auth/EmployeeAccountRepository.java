package robert_neat.identity_access_mock.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeAccountRepository extends JpaRepository<EmployeeAccount, String> {

    Optional<EmployeeAccount> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    List<EmployeeAccount> findAllByOrderByEmployeeIdAsc();
}
