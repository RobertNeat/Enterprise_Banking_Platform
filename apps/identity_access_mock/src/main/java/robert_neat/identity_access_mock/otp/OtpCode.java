package robert_neat.identity_access_mock.otp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity for an OTP code. The plaintext code is only ever exposed via the operator UI (templates/otp). */
@Entity
@Table(name = "otp_code", schema = "identity_access")
public class OtpCode {

    @Id
    @Column(name = "otp_id", length = 36)
    private String otpId;

    @Column(name = "employee_id", length = 50, nullable = false)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 20, nullable = false)
    private OtpPurpose purpose;

    @Column(name = "code", length = 6, nullable = false)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    protected OtpCode() {
        // JPA
    }

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

    @jakarta.persistence.Transient
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
