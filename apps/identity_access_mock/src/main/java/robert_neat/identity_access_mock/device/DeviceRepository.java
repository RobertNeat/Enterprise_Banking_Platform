package robert_neat.identity_access_mock.device;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findByOwnerIdOrderByDeviceIdAsc(String ownerId);

    List<Device> findAllByOrderByDeviceIdAsc();

    /** Pessimistic write lock so unbind()/reset() mutations on the same device don't race. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Device d where d.deviceId = :deviceId")
    Optional<Device> findByIdForUpdate(String deviceId);
}
