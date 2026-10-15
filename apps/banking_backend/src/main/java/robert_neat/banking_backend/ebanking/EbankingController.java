package robert_neat.banking_backend.ebanking;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ebanking")
public class EbankingController {

    private final EbankingService ebankingService;
    private final EbankingActionsService ebankingActionsService;

    public EbankingController(EbankingService ebankingService, EbankingActionsService ebankingActionsService) {
        this.ebankingService = ebankingService;
        this.ebankingActionsService = ebankingActionsService;
    }

    @GetMapping("/users")
    public List<EbankingUserDto> findAll() {
        return ebankingService.findAll();
    }

    /** F5 step 1 — issues an UNLOCK_ACCESS OTP challenge via identity_access_mock. */
    @PostMapping("/{userId}/unlock/request-otp")
    public OtpChallengeResponse requestUnlockOtp(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        return ebankingActionsService.requestUnlockOtp(userId, jwt.getSubject());
    }

    /** F5 step 2 — verifies the OTP and, if valid, unlocks the e-banking access (409 if already active). */
    @PostMapping("/{userId}/unlock/confirm")
    public EbankingUserDto confirmUnlock(
            @PathVariable String userId,
            @RequestBody OtpConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ebankingActionsService.confirmUnlock(userId, request, jwt.getSubject());
    }

    /** Proxy to identity_access_mock's device registry — lets the frontend pick a device to reset. */
    @GetMapping("/{userId}/devices")
    public List<EbankingDeviceDto> listDevices(@PathVariable String userId) {
        return ebankingActionsService.listDevices(userId);
    }

    /** F5 step 1 (device reset) — issues a RESET_DEVICE OTP challenge. */
    @PostMapping("/{userId}/devices/{deviceId}/reset/request-otp")
    public OtpChallengeResponse requestDeviceResetOtp(
            @PathVariable String userId, @PathVariable String deviceId, @AuthenticationPrincipal Jwt jwt) {
        return ebankingActionsService.requestDeviceResetOtp(userId, deviceId, jwt.getSubject());
    }

    /** F5 step 2 (device reset) — verifies the OTP and, if valid, resets/unbinds the device. */
    @PostMapping("/{userId}/devices/{deviceId}/reset/confirm")
    public void confirmDeviceReset(
            @PathVariable String userId,
            @PathVariable String deviceId,
            @RequestBody OtpConfirmRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ebankingActionsService.confirmDeviceReset(userId, deviceId, request, jwt.getSubject());
    }
}
