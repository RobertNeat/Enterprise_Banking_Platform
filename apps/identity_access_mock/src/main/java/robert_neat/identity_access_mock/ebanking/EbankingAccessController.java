package robert_neat.identity_access_mock.ebanking;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.identity_access_mock.common.ForbiddenException;
import robert_neat.identity_access_mock.otp.OtpPurpose;
import robert_neat.identity_access_mock.otp.OtpService;

/**
 * F5: confirms strong-auth unlock for the e-banking channel. The EbankingUser entity (accessStatus)
 * lives in banking_backend; this mock only certifies that a verified OTP backs the unlock decision.
 */
@RestController
@RequestMapping("/ebanking-access")
public class EbankingAccessController {

    private final OtpService otpService;

    public EbankingAccessController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/{userId}/unlock")
    public UnlockResponse unlock(@PathVariable String userId, @RequestBody UnlockRequest request) {
        if (!otpService.consumeVerified(request.otpId(), OtpPurpose.UNLOCK_ACCESS)) {
            throw new ForbiddenException("E-banking unlock requires a verified, unused UNLOCK_ACCESS OTP");
        }
        return new UnlockResponse(true);
    }
}
