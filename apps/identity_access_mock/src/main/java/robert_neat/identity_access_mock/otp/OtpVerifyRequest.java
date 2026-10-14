package robert_neat.identity_access_mock.otp;

public record OtpVerifyRequest(String otpId, String code) {
}
