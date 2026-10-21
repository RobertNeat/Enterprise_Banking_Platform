package robert_neat.identity_access_mock.otp;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.identity_access_mock.common.NotFoundException;
import robert_neat.identity_access_mock.common.ValidationException;

/** OTP/SCA store, persisted via JPA. Codes expire after a short TTL and are consumed (marked verified) on success. */
@Service
public class OtpService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final OtpCodeRepository otpCodeRepository;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    @Transactional
    public OtpRequestResponse requestOtp(OtpRequestRequest request) {
        if (request.employeeId() == null || request.purpose() == null) {
            throw new ValidationException("employeeId and purpose are required");
        }
        String otpId = UUID.randomUUID().toString();
        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(TTL);
        otpCodeRepository.save(new OtpCode(otpId, request.employeeId(), request.purpose(), code, expiresAt));
        return new OtpRequestResponse(otpId, expiresAt);
    }

    /**
     * Locks the row for the duration of the transaction (see OtpCodeRepository.findByIdForUpdate)
     * so a concurrent consumeVerified() can't flush a stale consumed=false over it.
     */
    @Transactional
    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        if (request.otpId() == null) {
            return new OtpVerifyResponse(false);
        }
        Optional<OtpCode> otp = otpCodeRepository.findByIdForUpdate(request.otpId());
        if (otp.isEmpty()) {
            return new OtpVerifyResponse(false);
        }
        OtpCode code = otp.get();
        boolean matches = !code.isExpired() && !code.isConsumed() && code.getCode().equals(request.code());
        if (matches) {
            code.setVerified(true);
        }
        return new OtpVerifyResponse(matches);
    }

    /** Used by device/e-banking flows to enforce "verified OTP for the right purpose" before acting. */
    @Transactional(readOnly = true)
    public boolean isVerified(String otpId, OtpPurpose expectedPurpose) {
        if (otpId == null) {
            return false;
        }
        return otpCodeRepository.findById(otpId)
                .map(otp -> otp.isVerified() && !otp.isConsumed() && otp.getPurpose() == expectedPurpose)
                .orElse(false);
    }

    /**
     * Consumes a verified OTP so it cannot be replayed for a second unlock/reset.
     * Returns true only on the first successful use for the given purpose. Locks the row so two
     * concurrent calls can't both pass the check and both consume it.
     */
    @Transactional
    public boolean consumeVerified(String otpId, OtpPurpose expectedPurpose) {
        if (otpId == null) {
            return false;
        }
        Optional<OtpCode> otp = otpCodeRepository.findByIdForUpdate(otpId);
        if (otp.isEmpty()) {
            return false;
        }
        OtpCode code = otp.get();
        if (!code.isVerified() || code.isConsumed() || code.getPurpose() != expectedPurpose) {
            return false;
        }
        code.setConsumed(true);
        return true;
    }

    @Transactional(readOnly = true)
    public OtpCode getOrThrow(String otpId) {
        return otpCodeRepository.findById(otpId)
                .orElseThrow(() -> new NotFoundException("OTP not found: " + otpId));
    }

    @Transactional(readOnly = true)
    public List<OtpCode> listAll() {
        return otpCodeRepository.findAllByOrderByExpiresAtAsc();
    }
}
