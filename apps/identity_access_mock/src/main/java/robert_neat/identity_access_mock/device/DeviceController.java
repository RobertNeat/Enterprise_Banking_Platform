package robert_neat.identity_access_mock.device;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** {id} is an owner id here: an employeeId or an e-banking customer/user id. */
    @GetMapping("/{ownerId}")
    public List<Device> byOwner(@PathVariable String ownerId) {
        return deviceService.listByOwner(ownerId);
    }

    /** {id} is a deviceId here (e.g. DEV-0001). */
    @PostMapping("/{deviceId}/unbind")
    public ResponseEntity<Void> unbind(@PathVariable String deviceId) {
        deviceService.unbind(deviceId);
        return ResponseEntity.noContent().build();
    }

    /** {id} is a deviceId here (e.g. DEV-0001); requires a previously verified RESET_DEVICE OTP. */
    @PostMapping("/{deviceId}/reset")
    public ResponseEntity<Void> reset(@PathVariable String deviceId, @RequestBody DeviceResetRequest request) {
        deviceService.reset(deviceId, request.otpId());
        return ResponseEntity.noContent().build();
    }
}
