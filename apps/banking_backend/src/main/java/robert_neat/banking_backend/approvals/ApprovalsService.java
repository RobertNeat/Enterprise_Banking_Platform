package robert_neat.banking_backend.approvals;

import org.springframework.stereotype.Service;
import robert_neat.banking_backend.auth.RoleCode;
import robert_neat.banking_backend.auth.UnmappedRoleException;
import robert_neat.banking_backend.customer.Employee;
import robert_neat.banking_backend.customer.EmployeeRepository;

import java.util.List;
import java.util.Optional;

/**
 * Proxies governance_mock's approval queue and enforces the maker-checker seniority rule on the
 * backend (not just the UI): a checker may decide an approval only if their seniorityLevel is
 * strictly greater than the requester's AND they are not the requester. requestedBy is an
 * employeeId (PRC-xxxxx); its role is resolved from banking_backend's own Employee table (same
 * PRC-xxxxx ids as identity_access_mock's demo accounts — see db/changelog/seed/001-customer.sql)
 * rather than calling identity_access_mock, avoiding an extra HTTP round trip per request.
 */
@Service
public class ApprovalsService {

    private final GovernanceClient governanceClient;
    private final EmployeeRepository employeeRepository;

    public ApprovalsService(GovernanceClient governanceClient, EmployeeRepository employeeRepository) {
        this.governanceClient = governanceClient;
        this.employeeRepository = employeeRepository;
    }

    public List<ApprovalRecord> list(String status) {
        return governanceClient.listApprovals(status).stream().map(this::withRequester).toList();
    }

    public ApprovalRecord get(String id) {
        return withRequester(governanceClient.getApproval(id));
    }

    public ApprovalRecord decide(String id, String decision, String checkerEmployeeId, int checkerSeniorityLevel) {
        ApprovalRecord approval = governanceClient.getApproval(id);

        if (checkerEmployeeId.equals(approval.requestedBy())) {
            throw new InsufficientSeniorityException(
                    "Nie mozna zatwierdzic wlasnego wniosku (self-approval)");
        }

        RoleCode requesterRole = resolveRole(approval.requestedBy())
                .orElseThrow(() -> new InsufficientSeniorityException(
                        "Nie mozna ustalic roli wnioskodawcy " + approval.requestedBy()));

        if (checkerSeniorityLevel <= requesterRole.seniorityLevel()) {
            throw new InsufficientSeniorityException(
                    "Niewystarczajacy poziom uprawnien do zatwierdzenia tego wniosku");
        }

        return withRequester(governanceClient.decideApproval(id, decision, checkerEmployeeId));
    }

    private ApprovalRecord withRequester(ApprovalRecord approval) {
        Optional<RoleCode> role = resolveRole(approval.requestedBy());
        return approval.withRequester(
                role.map(RoleCode::displayName).orElse(null),
                role.map(RoleCode::seniorityLevel).orElse(null));
    }

    private Optional<RoleCode> resolveRole(String employeeId) {
        return employeeRepository.findById(employeeId)
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
