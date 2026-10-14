package robert_neat.identity_access_mock.otp;

import java.time.Instant;

/** In-memory OTP record. The plaintext code is only ever exposed via the operator UI (templates/otp). */
public class OtpCode {

    private final String otpId;
    private final String employeeId;
    private final OtpPurpose purpose;
    private final String code;
    private final Instant expiresAt;
    private boolean verified;
    private boolean consumed;

    public OtpCode(String otpId, String employeeId, OtpPurpose purpose, String code, Instant expiresAt) {
        this.otpId = otpId;
        this.employeeId = employeeId;
        this.purpose = purpose;
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.consumed = false;
    }

    public String getOtpId() {
        return otpId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public String getCode() {
        return code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
