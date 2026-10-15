package robert_neat.banking_backend.ebanking;

import java.time.Instant;

/** Returned from request-otp endpoints — the code itself is never exposed here, only in identity_access_mock's Thymeleaf panel (demo). */
public record OtpChallengeResponse(String otpId, Instant expiresAt) {
}
