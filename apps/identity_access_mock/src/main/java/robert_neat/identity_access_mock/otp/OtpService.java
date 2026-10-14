package robert_neat.identity_access_mock.otp;

import org.springframework.stereotype.Service;
import robert_neat.identity_access_mock.common.NotFoundException;
import robert_neat.identity_access_mock.common.ValidationException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory OTP/SCA store. Codes expire after a short TTL and are consumed (marked verified) on success. */
@Service
public class OtpService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final Map<String, OtpCode> otpsById = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public OtpRequestResponse requestOtp(OtpRequestRequest request) {
        if (request.employeeId() == null || request.purpose() == null) {
            throw new ValidationException("employeeId and purpose are required");
        }
        String otpId = UUID.randomUUID().toString();
        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(TTL);
        otpsById.put(otpId, new OtpCode(otpId, request.employeeId(), request.purpose(), code, expiresAt));
        return new OtpRequestResponse(otpId, expiresAt);
    }

    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        OtpCode otp = otpsById.get(request.otpId());
        if (otp == null) {
            return new OtpVerifyResponse(false);
        }
        boolean matches = !otp.isExpired() && !otp.isConsumed() && otp.getCode().equals(request.code());
        if (matches) {
            otp.setVerified(true);
        }
        return new OtpVerifyResponse(matches);
    }

    /** Used by device/e-banking flows to enforce "verified OTP for the right purpose" before acting. */
    public boolean isVerified(String otpId, OtpPurpose expectedPurpose) {
        if (otpId == null) {
            return false;
        }
        OtpCode otp = otpsById.get(otpId);
        return otp != null && otp.isVerified() && !otp.isConsumed() && otp.getPurpose() == expectedPurpose;
    }

    /**
     * Consumes a verified OTP so it cannot be replayed for a second unlock/reset.
     * Returns true only on the first successful use for the given purpose.
     */
    public boolean consumeVerified(String otpId, OtpPurpose expectedPurpose) {
        if (!isVerified(otpId, expectedPurpose)) {
            return false;
        }
        otpsById.get(otpId).setConsumed(true);
        return true;
    }

    public OtpCode getOrThrow(String otpId) {
        OtpCode otp = otpsById.get(otpId);
        if (otp == null) {
            throw new NotFoundException("OTP not found: " + otpId);
        }
        return otp;
    }

    public Collection<OtpCode> listAll() {
        return otpsById.values();
    }
}
