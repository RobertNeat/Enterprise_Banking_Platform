package robert_neat.banking_backend.auth;

import java.time.Instant;

/** Mirrors identity_access_mock's device registry entries (GET /devices/{ownerId}). */
public record DeviceResponse(String deviceId, String ownerId, String model, Instant lastSeenAt, boolean trusted) {
}
