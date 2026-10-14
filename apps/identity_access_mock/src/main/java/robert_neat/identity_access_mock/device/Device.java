package robert_neat.identity_access_mock.device;

import java.time.Instant;

/** Trusted mobile device registered against an employeeId or an e-banking customer/user id. */
public class Device {

    private final String deviceId;
    private final String ownerId;
    private final String model;
    private Instant lastSeenAt;
    private boolean trusted;

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
