package robert_neat.banking_backend.approvals;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import robert_neat.banking_backend.auth.RoleCode;
import robert_neat.banking_backend.auth.UnmappedRoleException;
import robert_neat.banking_backend.common.BankTimeZone;
import robert_neat.banking_backend.customer.Employee;
import robert_neat.banking_backend.customer.EmployeeRepository;

/**
 * Proxies governance_mock's approval queue and enforces the maker-checker seniority rule on the
 * backend (not just the UI): a checker may decide an approval only if their seniorityLevel is
 * strictly greater than the requester's AND they are not the requester. requestedBy is an
 * employeeId (PRC-xxxxx); its role is resolved from banking_backend's own Employee table (same
 * PRC-xxxxx ids as identity_access_mock's demo accounts — see db/changelog/seed/001-customer.sql)
 * rather than calling identity_access_mock, avoiding an extra HTTP round trip per request.
 *
 * <p>requireCheckerExists() is the maker-side counterpart to decide()'s seniority check: a role
 * with no strictly-higher role in the system (currently only ADMIN) can never have its approval
 * decided by anyone, so it must be blocked from creating one at all — otherwise the approval is
 * stuck PENDING forever. Callers (LoanActionsService.submitForApproval,
 * EnforcementActionsService.requestTransferApproval) must call this before
 * governanceClient.createApproval(), after their own stage/status preconditions so an invalid
 * request still gets its usual 409/404 rather than being masked by a 403.
 */
@Service
public class ApprovalsService {

  private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(BankTimeZone.ZONE);

  private final GovernanceClient governanceClient;
  private final EmployeeRepository employeeRepository;

  public ApprovalsService(
      GovernanceClient governanceClient, EmployeeRepository employeeRepository) {
    this.governanceClient = governanceClient;
    this.employeeRepository = employeeRepository;
  }

  public List<ApprovalRecord> list(String status) {
    return governanceClient.listApprovals(status).stream().map(this::withRequester).toList();
  }

  public ApprovalRecord get(String id) {
    return withRequester(governanceClient.getApproval(id));
  }

  /**
   * Read-side of the audit trail ("Historia" dialog) — otherwise a straight proxy, the only
   * transformation is formatting timestamp to Europe/Warsaw local time (see BankTimeZone) so the
   * frontend doesn't need its own Instant/timezone handling, matching EbankingUserDto.lastLogin.
   */
  public List<AuditEventDto> listAuditEvents(String entityType, String entityId) {
    return governanceClient.listAuditEvents(entityType, entityId).stream()
        .map(
            e ->
                new AuditEventDto(
                    e.id(),
                    e.actor(),
                    e.action(),
                    e.entityType(),
                    e.entityId(),
                    AUDIT_TIMESTAMP_FORMAT.format(e.timestamp()),
                    e.details()))
        .toList();
  }

  public ApprovalRecord decide(
      String id, String decision, String checkerEmployeeId, int checkerSeniorityLevel) {
    ApprovalRecord approval = governanceClient.getApproval(id);

    if (checkerEmployeeId.equals(approval.requestedBy())) {
      throw new InsufficientSeniorityException(
          "Nie mozna zatwierdzic wlasnego wniosku (self-approval)");
    }

    RoleCode requesterRole =
        resolveRole(approval.requestedBy())
            .orElseThrow(
                () ->
                    new InsufficientSeniorityException(
                        "Nie mozna ustalic roli wnioskodawcy " + approval.requestedBy()));

    if (checkerSeniorityLevel <= requesterRole.seniorityLevel()) {
      throw new InsufficientSeniorityException(
          "Niewystarczajacy poziom uprawnien do zatwierdzenia tego wniosku");
    }

    return withRequester(governanceClient.decideApproval(id, decision, checkerEmployeeId));
  }

  /**
   * @throws InsufficientSeniorityException if requesterEmployeeId's role has no strictly-higher
   *     role in the system (i.e. no one could ever check an approval it requests). An unmapped or
   *     unresolvable role does NOT throw here — that is a data problem surfaced later, at decide()
   *     time, not a reason to block submission.
   */
  public void requireCheckerExists(String requesterEmployeeId) {
    resolveRole(requesterEmployeeId)
        .ifPresent(
            role -> {
              if (!role.hasHigherSeniorityRole()) {
                throw new InsufficientSeniorityException(
                    "Rola "
                        + role.displayName()
                        + " ma najwyzszy poziom uprawnien w systemie — "
                        + "nie ma kto zatwierdzic tego wniosku (maker-checker)");
              }
            });
  }

  private ApprovalRecord withRequester(ApprovalRecord approval) {
    Optional<RoleCode> role = resolveRole(approval.requestedBy());
    return approval.withRequester(
        role.map(RoleCode::displayName).orElse(null),
        role.map(RoleCode::seniorityLevel).orElse(null));
  }

  private Optional<RoleCode> resolveRole(String employeeId) {
    return employeeRepository
        .findById(employeeId)
        .map(Employee::getRole)
        .flatMap(this::safeFromDisplayName);
  }

  private Optional<RoleCode> safeFromDisplayName(String displayName) {
    try {
      return Optional.of(RoleCode.fromDisplayName(displayName));
    } catch (UnmappedRoleException ex) {
      return Optional.empty();
    }
  }
}
