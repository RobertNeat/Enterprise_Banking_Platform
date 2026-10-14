package robert_neat.identity_access_mock.otp;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request")
    public OtpRequestResponse request(@RequestBody OtpRequestRequest request) {
        return otpService.requestOtp(request);
    }

    @PostMapping("/verify")
    public OtpVerifyResponse verify(@RequestBody OtpVerifyRequest request) {
        return otpService.verify(request);
    }
}
