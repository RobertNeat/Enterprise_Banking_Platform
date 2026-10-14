package robert_neat.identity_access_mock.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import robert_neat.identity_access_mock.auth.AuthService;
import robert_neat.identity_access_mock.device.DeviceService;
import robert_neat.identity_access_mock.otp.OtpService;

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
}
