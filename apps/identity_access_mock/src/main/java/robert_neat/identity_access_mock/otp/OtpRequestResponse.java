package robert_neat.identity_access_mock.otp;

import java.time.Instant;

public record OtpRequestResponse(String otpId, Instant expiresAt) {
}
