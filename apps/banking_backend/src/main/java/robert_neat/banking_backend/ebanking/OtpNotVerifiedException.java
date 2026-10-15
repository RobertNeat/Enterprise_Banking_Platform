package robert_neat.banking_backend.ebanking;

/**
 * Thrown when identity_access_mock's POST /otp/verify returns {"verified": false} (wrong code,
 * expired, already used, or unknown otpId — see docs/identity_access_mock_contract/api.md).
 * Distinct from IdentityAccessException: this is not an HTTP error from the mock, it's a 200
 * response the caller must still reject as a failed SCA challenge.
 */
public class OtpNotVerifiedException extends RuntimeException {

    public OtpNotVerifiedException(String message) {
        super(message);
    }
}
