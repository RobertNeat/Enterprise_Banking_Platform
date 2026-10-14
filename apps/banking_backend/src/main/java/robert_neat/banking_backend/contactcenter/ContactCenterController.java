package robert_neat.banking_backend.contactcenter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contact-center")
public class ContactCenterController {

    private final ContactCenterService contactCenterService;

    public ContactCenterController(ContactCenterService contactCenterService) {
        this.contactCenterService = contactCenterService;
    }

    @GetMapping("/dispositions")
    public List<ServiceDispositionDto> findDispositions() {
        return contactCenterService.findAllDispositions();
    }

    @GetMapping("/history")
    public List<ContactHistoryEntryDto> findHistory() {
        return contactCenterService.findAllHistory();
    }

    @GetMapping("/profile/{customerId}")
    public CustomerProfileDto findProfile(@PathVariable String customerId) {
        return contactCenterService.findProfile(customerId);
    }
}
