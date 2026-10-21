package robert_neat.identity_access_mock.otp;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    List<OtpCode> findAllByOrderByExpiresAtAsc();

    /**
     * Pessimistic write lock so verify()/consumeVerified() calls for the same OTP can't race:
     * without it, a re-verify loading a stale consumed=false could flush after a concurrent
     * consume and silently un-consume the row, allowing replay.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtpCode o where o.otpId = :otpId")
    Optional<OtpCode> findByIdForUpdate(String otpId);
}
