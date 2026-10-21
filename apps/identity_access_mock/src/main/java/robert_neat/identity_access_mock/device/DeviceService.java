package robert_neat.identity_access_mock.device;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.identity_access_mock.common.ForbiddenException;
import robert_neat.identity_access_mock.common.NotFoundException;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpService;

/** Device registry, persisted via JPA. Seeded with sample mobile devices for demo employees/e-banking users. */
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final OtpService otpService;

    public DeviceService(DeviceRepository deviceRepository, OtpService otpService) {
        this.deviceRepository = deviceRepository;
        this.otpService = otpService;
    }

    @Transactional(readOnly = true)
    public List<Device> listByOwner(String ownerId) {
        return deviceRepository.findByOwnerIdOrderByDeviceIdAsc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Device> listAll() {
        return deviceRepository.findAllByOrderByDeviceIdAsc();
    }

    @Transactional
    public void unbind(String deviceId) {
        Device device = getOrThrowForUpdate(deviceId);
        device.setTrusted(false);
    }

    @Transactional
    public void reset(String deviceId, String otpId) {
        Device device = getOrThrowForUpdate(deviceId);
        if (!otpService.consumeVerified(otpId, OtpPurpose.RESET_DEVICE)) {
            throw new ForbiddenException("Device reset requires a verified, unused RESET_DEVICE OTP");
        }
        device.setLastSeenAt(Instant.now());
        device.setTrusted(true);
    }

    private Device getOrThrowForUpdate(String deviceId) {
        return deviceRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));
    }
}
