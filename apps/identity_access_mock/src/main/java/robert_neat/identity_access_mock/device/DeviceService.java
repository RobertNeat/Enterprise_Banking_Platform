package robert_neat.identity_access_mock.device;

import org.springframework.stereotype.Service;
import robert_neat.identity_access_mock.common.ForbiddenException;
import robert_neat.identity_access_mock.common.NotFoundException;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory device registry, seeded with sample mobile devices for demo employees/e-banking users. */
@Service
public class DeviceService {

    private final Map<String, Device> devicesById = new ConcurrentHashMap<>();
    private final OtpService otpService;

    public DeviceService(OtpService otpService) {
        this.otpService = otpService;
        seedDevices();
    }

    private void seedDevices() {
        devicesById.put("DEV-0001", new Device("DEV-0001", "PRC-30678", "iPhone 14", Instant.now(), true));
        devicesById.put("DEV-0002", new Device("DEV-0002", "EB-1001", "Samsung Galaxy S23", Instant.now(), true));
        devicesById.put("DEV-0003", new Device("DEV-0003", "EB-1004", "Xiaomi Redmi Note 12", Instant.now(), false));
    }

    public List<Device> listByOwner(String ownerId) {
        return devicesById.values().stream().filter(d -> d.getOwnerId().equals(ownerId)).toList();
    }

    public List<Device> listAll() {
        return List.copyOf(devicesById.values());
    }

    public void unbind(String deviceId) {
        Device device = getOrThrow(deviceId);
        device.setTrusted(false);
    }

    public void reset(String deviceId, String otpId) {
        Device device = getOrThrow(deviceId);
        if (!otpService.consumeVerified(otpId, OtpPurpose.RESET_DEVICE)) {
            throw new ForbiddenException("Device reset requires a verified, unused RESET_DEVICE OTP");
        }
        device.setLastSeenAt(Instant.now());
        device.setTrusted(true);
    }

    private Device getOrThrow(String deviceId) {
        Device device = devicesById.get(deviceId);
        if (device == null) {
            throw new NotFoundException("Device not found: " + deviceId);
        }
        return device;
    }
}
