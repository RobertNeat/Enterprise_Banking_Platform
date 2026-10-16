package robert_neat.identity_access_mock.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import robert_neat.identity_access_mock.auth.AuthService;
import robert_neat.identity_access_mock.device.DeviceService;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpService;
import robert_neat.identity_access_mock.otp.OtpVerifyRequest;

/** Manual-testing panel: separate from the REST API, reads the same in-memory services. */
@Controller
public class UiController {

    private final AuthService authService;
    private final OtpService otpService;
    private final DeviceService deviceService;

    public UiController(AuthService authService, OtpService otpService, DeviceService deviceService) {
        this.authService = authService;
        this.otpService = otpService;
        this.deviceService = deviceService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", authService.listAccounts());
        return "accounts/index";
    }

    @GetMapping("/otp")
    public String otp(Model model) {
        model.addAttribute("otps", otpService.listAll());
        return "otp/index";
    }

    @GetMapping("/devices")
    public String devices(Model model) {
        model.addAttribute("devices", deviceService.listAll());
        return "devices/index";
    }

    /** Post/redirect/get: avoids returning raw REST JSON to a browser form submit. */
    @PostMapping("/devices/{deviceId}/unbind-ui")
    public String unbindFromUi(@PathVariable String deviceId) {
        deviceService.unbind(deviceId);
        return "redirect:/devices";
    }

    @GetMapping("/ebanking")
    public String ebanking() {
        return "ebanking/index";
    }

    /**
     * Post/redirect/get: a browser form can't fill a path variable, so this takes plain request
     * params (unlike the REST endpoint) and does verify+consume in one step so the operator only
     * needs the OTP code shown on /otp, not a separate call to /otp/verify.
     */
    @PostMapping("/ebanking/unlock-ui")
    public String unlockFromUi(@RequestParam String userId, @RequestParam String otpId,
                                @RequestParam String code, RedirectAttributes redirectAttributes) {
        boolean unlocked = otpService.verify(new OtpVerifyRequest(otpId, code)).verified()
                && otpService.consumeVerified(otpId, OtpPurpose.UNLOCK_ACCESS);
        String result = unlocked
                ? "Dostęp e-banking odblokowany dla " + userId
                : "Odblokowanie nie powiodło się — nieprawidłowy, niezweryfikowany lub zużyty OTP";
        redirectAttributes.addFlashAttribute("result", result);
        return "redirect:/ebanking";
    }
}
