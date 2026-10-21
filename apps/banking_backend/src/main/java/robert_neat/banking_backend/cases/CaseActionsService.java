package robert_neat.banking_backend.cases;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.approvals.SlaCheckResult;
import robert_neat.banking_backend.common.BankTimeZone;
import robert_neat.banking_backend.common.InvalidRequestException;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.customer.Customer;
import robert_neat.banking_backend.customer.CustomerRepository;
import robert_neat.banking_backend.customer.Employee;
import robert_neat.banking_backend.customer.EmployeeRepository;

/**
 * F8 — case registration computes the statutory deadline and SLA instead of accepting them from the
 * wizard (see docs/banking_web_functions(concise).md, "Ustawa o rozpatrywaniu reklamacji..."): 30
 * days from registration by default, extended to 60 for case types deemed "especially complex"
 * (restructuring requests — multi-party, requires credit committee input, unlike a plain complaint
 * or status inquiry). SLA-to-escalation is a separate, shorter clock keyed by priority: high=24h,
 * medium=72h, low=120h — these are not in any contract, chosen as a reasonable internal
 * responsiveness target distinct from the legal deadline (see docs/services/banking_backend.md).
 */
@Service
@Transactional
public class CaseActionsService {

  private static final Set<String> COMPLEX_CASE_TYPES = Set.of("restructuring_request");
  private static final int STANDARD_DEADLINE_DAYS = 30;
  private static final int COMPLEX_DEADLINE_DAYS = 60;

  /**
   * Must mirror the CHECK constraint on customer_case.priority (db/changelog/schema/006-cases.sql).
   */
  private static final List<String> VALID_PRIORITIES = List.of("low", "medium", "high");

  /**
   * Must mirror the ck_customer_case_case_type CHECK constraint (db/changelog/schema/006-cases.sql,
   * changeset customer_case-002) and CASE_TYPES in banking_web cases.page.ts. Validated here so an
   * invalid value is rejected as 400, not as a raw DataIntegrityViolationException (500) from the
   * DB constraint.
   */
  private static final List<String> VALID_CASE_TYPES =
      List.of("complaint", "restructuring_request", "inquiry");

  private final CustomerCaseRepository customerCases;
  private final CustomerRepository customers;
  private final EmployeeRepository employees;
  private final GovernanceClient governanceClient;
  private final CaseIdGenerator idGenerator;
  private final CaseService caseService;

  public CaseActionsService(
      CustomerCaseRepository customerCases,
      CustomerRepository customers,
      EmployeeRepository employees,
      GovernanceClient governanceClient,
      CaseIdGenerator idGenerator,
      CaseService caseService) {
    this.customerCases = customerCases;
    this.customers = customers;
    this.employees = employees;
    this.governanceClient = governanceClient;
    this.idGenerator = idGenerator;
    this.caseService = caseService;
  }

  public CustomerCaseDto create(CreateCustomerCaseRequest request, String callerEmployeeId) {
    validateCreateRequest(request);

    Customer customer =
        customers
            .findById(request.customerId())
            .orElseThrow(
                () -> new NotFoundException("Customer not found: " + request.customerId()));
    String ownerId =
        request.ownerId() != null && !request.ownerId().isBlank()
            ? request.ownerId()
            : callerEmployeeId;
    Employee owner =
        employees
            .findById(ownerId)
            .orElseThrow(() -> new NotFoundException("Employee not found: " + ownerId));

    Instant now = Instant.now();
    int deadlineDays =
        COMPLEX_CASE_TYPES.contains(request.caseType())
            ? COMPLEX_DEADLINE_DAYS
            : STANDARD_DEADLINE_DAYS;
    LocalDate statutoryDeadline =
        now.atZone(BankTimeZone.ZONE).toLocalDate().plusDays(deadlineDays);
    Instant slaDueAt = now.plus(Duration.ofHours(slaHoursFor(request.priority())));

    CustomerCase customerCase = new CustomerCase();
    customerCase.setId(idGenerator.next());
    customerCase.setCustomer(customer);
    customerCase.setSubject(request.subject());
    customerCase.setOwner(owner);
    customerCase.setPriority(request.priority());
    customerCase.setCaseType(request.caseType());
    customerCase.setRegisteredAt(now);
    customerCase.setStatutoryDeadline(statutoryDeadline);
    customerCase.setSlaDueAt(slaDueAt);
    customerCase.setStatus("new");
    customerCases.save(customerCase);

    return CaseService.toDto(customerCase, now);
  }

  /**
   * Must run before any side effect (DB write) — an invalid priority would otherwise surface as a
   * raw DataIntegrityViolationException (500) when the DB's CHECK constraint rejects the insert, or
   * as an IllegalArgumentException (500) from slaHoursFor's switch.
   */
  private void validateCreateRequest(CreateCustomerCaseRequest request) {
    if (request.priority() == null || !VALID_PRIORITIES.contains(request.priority())) {
      throw new InvalidRequestException(
          "priority must be one of: " + String.join(", ", VALID_PRIORITIES));
    }
    if (request.caseType() == null || request.caseType().isBlank()) {
      throw new InvalidRequestException("caseType must not be blank");
    }
    if (!VALID_CASE_TYPES.contains(request.caseType())) {
      throw new InvalidRequestException(
          "caseType must be one of: " + String.join(", ", VALID_CASE_TYPES));
    }
  }

  public EscalateCaseResponse escalate(String caseId, String employeeId) {
    CustomerCase customerCase =
        customerCases
            .findById(caseId)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseId));
    if ("resolved".equals(customerCase.getStatus())
        || "escalated".equals(customerCase.getStatus())) {
      throw new InvalidStageTransitionException(
          "Case " + caseId + " cannot be escalated from status " + customerCase.getStatus());
    }

    Instant now = Instant.now();
    long hoursRemaining = Duration.between(now, customerCase.getSlaDueAt()).toHours();
    SlaCheckResult slaCheck = governanceClient.slaCheck(caseId, hoursRemaining);

    if (slaCheck.escalated()) {
      // governance_mock already wrote the AUTO_ESCALATED audit event for this case — do not
      // duplicate it (see docs/governance_mock_contract/api.md, POST /cases/{caseId}/sla-check).
      applyAutoEscalation(customerCase, now, slaCheck.auditEventId());
    } else {
      // Manual escalation while SLA has not yet expired — governance_mock does not audit this
      // case, so banking_backend records its own audit event for the decision.
      String auditEventId =
          governanceClient.recordAuditEvent(
              employeeId,
              "CASE_ESCALATED_MANUALLY",
              "CUSTOMER_CASE",
              caseId,
              "slaRemainingHours=" + hoursRemaining);
      customerCase.setStatus("escalated");
      customerCase.setEscalatedAt(now);
      customerCase.setEscalationSource("MANUAL");
      customerCase.setGovernanceAuditEventId(auditEventId);
      customerCases.save(customerCase);
    }

    return new EscalateCaseResponse(CaseService.toDto(customerCase, now), slaCheck.escalated());
  }

  /**
   * CAS automatic SLA sweep (F8, scheduled by CaseSlaEscalationScheduler) — runs outside any
   * request/JWT context, so there is no employeeId to pass to governance_mock. Re-checks the SLA
   * itself (rather than trusting the caller's status filter alone) and only escalates when
   * governance_mock confirms escalated=true, which guarantees the AUTO_SLA path and makes the "who
   * escalated this" question moot: governance_mock already recorded the AUTO_ESCALATED audit event
   * under actor 'SYSTEM' (see governance_mock CaseSlaController), banking_backend never calls
   * recordAuditEvent itself for this path. If the SLA check unexpectedly returns escalated=false
   * (e.g. a race where the case was just resolved/started past the point of the sweep's query),
   * this is a silent no-op — the case is left untouched for the next sweep or a manual decision.
   */
  public void autoEscalateIfSlaExpired(String caseId) {
    CustomerCase customerCase =
        customerCases
            .findById(caseId)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseId));
    if ("resolved".equals(customerCase.getStatus())
        || "escalated".equals(customerCase.getStatus())) {
      return;
    }

    Instant now = Instant.now();
    long hoursRemaining = Duration.between(now, customerCase.getSlaDueAt()).toHours();
    SlaCheckResult slaCheck = governanceClient.slaCheck(caseId, hoursRemaining);
    if (slaCheck.escalated()) {
      applyAutoEscalation(customerCase, now, slaCheck.auditEventId());
    }
  }

  private void applyAutoEscalation(CustomerCase customerCase, Instant now, String auditEventId) {
    customerCase.setStatus("escalated");
    customerCase.setEscalatedAt(now);
    customerCase.setEscalationSource("AUTO_SLA");
    customerCase.setGovernanceAuditEventId(auditEventId);
    customerCases.save(customerCase);
  }

  /** CAS lifecycle — only 'new' may start; audited as CASE_STATUS_CHANGED in governance_mock. */
  public CustomerCaseDto start(String caseId, String employeeId) {
    CustomerCase customerCase =
        customerCases
            .findById(caseId)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseId));
    if (!"new".equals(customerCase.getStatus())) {
      throw new InvalidStageTransitionException(
          "Case " + caseId + " cannot be started from status " + customerCase.getStatus());
    }

    String fromStatus = customerCase.getStatus();
    governanceClient.recordAuditEvent(
        employeeId, "CASE_STATUS_CHANGED", "CUSTOMER_CASE", caseId, fromStatus + " -> inProgress");
    customerCase.setStatus("inProgress");
    customerCases.save(customerCase);

    return CaseService.toDto(customerCase, Instant.now());
  }

  /**
   * CAS lifecycle — 'new'/'inProgress'/'escalated' may resolve; 'resolved' is terminal (409). A
   * case never leaves 'escalated' other than through resolve, so escalation-SLA deduplication
   * (status filter in CaseSlaEscalationScheduler) keeps working.
   */
  public CustomerCaseDto resolve(String caseId, String employeeId) {
    CustomerCase customerCase =
        customerCases
            .findById(caseId)
            .orElseThrow(() -> new NotFoundException("Case not found: " + caseId));
    if ("resolved".equals(customerCase.getStatus())) {
      throw new InvalidStageTransitionException("Case " + caseId + " is already resolved");
    }

    String fromStatus = customerCase.getStatus();
    governanceClient.recordAuditEvent(
        employeeId, "CASE_STATUS_CHANGED", "CUSTOMER_CASE", caseId, fromStatus + " -> resolved");
    customerCase.setStatus("resolved");
    customerCases.save(customerCase);

    return CaseService.toDto(customerCase, Instant.now());
  }

  private long slaHoursFor(String priority) {
    return switch (priority) {
      case "high" -> 24;
      case "medium" -> 72;
      case "low" -> 120;
      default -> throw new IllegalArgumentException("Unknown priority: " + priority);
    };
  }
}
