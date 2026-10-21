package robert_neat.identity_access_mock.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity for a trusted mobile device registered against an employeeId or an e-banking customer/user id. */
@Entity
@Table(name = "device", schema = "identity_access")
public class Device {

    @Id
    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "owner_id", length = 50, nullable = false)
    private String ownerId;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "trusted", nullable = false)
    private boolean trusted;

    protected Device() {
        // JPA
    }

    public Device(String deviceId, String ownerId, String model, Instant lastSeenAt, boolean trusted) {
        this.deviceId = deviceId;
        this.ownerId = ownerId;
        this.model = model;
        this.lastSeenAt = lastSeenAt;
        this.trusted = trusted;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getModel() {
        return model;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }
}
