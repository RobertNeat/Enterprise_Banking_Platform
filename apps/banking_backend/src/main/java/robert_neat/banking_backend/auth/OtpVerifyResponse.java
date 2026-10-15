package robert_neat.banking_backend.auth;

/** Mirrors identity_access_mock's POST /otp/verify response — always HTTP 200. */
public record OtpVerifyResponse(boolean verified) {
}
