package robert_neat.banking_backend.ebanking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.auth.DeviceResponse;
import robert_neat.banking_backend.auth.IdentityAccessClient;
import robert_neat.banking_backend.auth.OtpRequestResponse;
import robert_neat.banking_backend.auth.OtpVerifyResponse;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;

import java.util.List;
import java.util.Set;

/**
 * F5 mutating flow — e-banking access unlock and device reset, both gated by OTP/SCA (PSD2)
 * verified through identity_access_mock. accessStatus preconditions are validated here before any
 * external call, same rationale as CardActionsService (R3): don't call the OTP-consuming endpoint
 * for an entity that is already in the target state.
 */
@Service
@Transactional
public class EbankingActionsService {

    private static final Set<String> UNLOCKABLE_STATUSES = Set.of("locked", "pendingReset");

    private final EbankingUserRepository ebankingUsers;
    private final IdentityAccessClient identityAccessClient;
    private final GovernanceClient governanceClient;
    private final EbankingService ebankingService;

    public EbankingActionsService(
            EbankingUserRepository ebankingUsers,
            IdentityAccessClient identityAccessClient,
            GovernanceClient governanceClient,
            EbankingService ebankingService) {
        this.ebankingUsers = ebankingUsers;
        this.identityAccessClient = identityAccessClient;
        this.governanceClient = governanceClient;
        this.ebankingService = ebankingService;
    }

    public OtpChallengeResponse requestUnlockOtp(String userId, String employeeId) {
        requireUnlockable(userId);
        OtpRequestResponse response = identityAccessClient.requestOtp(employeeId, "UNLOCK_ACCESS");
        return new OtpChallengeResponse(response.otpId(), response.expiresAt());
    }

    public EbankingUserDto confirmUnlock(String userId, OtpConfirmRequest request, String employeeId) {
        EbankingUser user = requireUnlockable(userId);

        OtpVerifyResponse verified = identityAccessClient.verifyOtp(request.otpId(), request.code());
        if (!verified.verified()) {
            throw new OtpNotVerifiedException("OTP code invalid or expired for otpId " + request.otpId());
        }

        identityAccessClient.unlockEbankingAccess(userId, request.otpId());

        user.setAccessStatus("active");
        ebankingUsers.save(user);
        governanceClient.recordAuditEvent(
                employeeId, "EBANKING_ACCESS_UNLOCKED", "EBANKING_USER", userId, null);

        return ebankingService.toDto(user);
    }

    public List<EbankingDeviceDto> listDevices(String userId) {
        requireUser(userId);
        return identityAccessClient.listDevices(userId).stream()
                .map(EbankingActionsService::toDeviceDto)
                .toList();
    }

    public OtpChallengeResponse requestDeviceResetOtp(String userId, String deviceId, String employeeId) {
        requireDevice(userId, deviceId);
        OtpRequestResponse response = identityAccessClient.requestOtp(employeeId, "RESET_DEVICE");
        return new OtpChallengeResponse(response.otpId(), response.expiresAt());
    }

    public void confirmDeviceReset(String userId, String deviceId, OtpConfirmRequest request, String employeeId) {
        requireDevice(userId, deviceId);

        OtpVerifyResponse verified = identityAccessClient.verifyOtp(request.otpId(), request.code());
        if (!verified.verified()) {
            throw new OtpNotVerifiedException("OTP code invalid or expired for otpId " + request.otpId());
        }

        identityAccessClient.resetDevice(deviceId, request.otpId());
        governanceClient.recordAuditEvent(employeeId, "DEVICE_RESET", "DEVICE", deviceId, "owner=" + userId);
    }

    private EbankingUser requireUnlockable(String userId) {
        EbankingUser user = requireUser(userId);
        if (!UNLOCKABLE_STATUSES.contains(user.getAccessStatus())) {
            throw new InvalidStageTransitionException(
                    "E-banking user " + userId + " is not locked/pendingReset (current: "
                            + user.getAccessStatus() + ")");
        }
        return user;
    }

    private EbankingUser requireUser(String userId) {
        return ebankingUsers.findById(userId)
                .orElseThrow(() -> new NotFoundException("E-banking user not found: " + userId));
    }

    /** Validates the device belongs to this user BEFORE trusting a {deviceId} path param across owners. */
    private DeviceResponse requireDevice(String userId, String deviceId) {
        requireUser(userId);
        return identityAccessClient.listDevices(userId).stream()
                .filter(d -> d.deviceId().equals(deviceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Device " + deviceId + " not found for e-banking user " + userId));
    }

    private static EbankingDeviceDto toDeviceDto(DeviceResponse d) {
        return new EbankingDeviceDto(d.deviceId(), d.ownerId(), d.model(), d.lastSeenAt(), d.trusted());
    }
}
