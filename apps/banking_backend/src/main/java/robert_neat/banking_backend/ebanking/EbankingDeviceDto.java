package robert_neat.banking_backend.ebanking;

import java.time.Instant;

/**
 * GET /ebanking/{userId}/devices response item — proxy of identity_access_mock's device registry.
 */
public record EbankingDeviceDto(
    String deviceId, String ownerId, String model, Instant lastSeenAt, boolean trusted) {}
