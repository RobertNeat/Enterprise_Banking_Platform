package robert_neat.banking_backend.auth;

import java.time.Instant;

/** Mirrors identity_access_mock's POST /otp/request response — code itself is never returned. */
public record OtpRequestResponse(String otpId, Instant expiresAt) {}
