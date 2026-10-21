package robert_neat.banking_backend.contactcenter;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contact-center")
public class ContactCenterController {

  private final ContactCenterService contactCenterService;

  public ContactCenterController(ContactCenterService contactCenterService) {
    this.contactCenterService = contactCenterService;
  }

  /**
   * KKT — customerId optional; omitted returns all dispositions (back-compat with existing
   * callers).
   */
  @GetMapping("/dispositions")
  public List<ServiceDispositionDto> findDispositions(
      @RequestParam(required = false) String customerId) {
    return contactCenterService.findAllDispositions(customerId);
  }

  /**
   * KKT — customerId optional; omitted returns all history entries (back-compat with existing
   * callers).
   */
  @GetMapping("/history")
  public List<ContactHistoryEntryDto> findHistory(
      @RequestParam(required = false) String customerId) {
    return contactCenterService.findAllHistory(customerId);
  }

  @GetMapping("/profile/{customerId}")
  public CustomerProfileDto findProfile(@PathVariable String customerId) {
    return contactCenterService.findProfile(customerId);
  }

  /**
   * KKT/F8 — links a service disposition to a case; 400 if caseId is blank or belongs to another
   * customer.
   */
  @PutMapping("/dispositions/{id}/case")
  public ServiceDispositionDto linkCase(
      @PathVariable String id, @RequestBody LinkDispositionCaseRequest request) {
    return contactCenterService.linkCase(id, request.caseId());
  }
}
