package robert_neat.banking_backend.collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.customer.Employee;
import robert_neat.banking_backend.customer.EmployeeRepository;

import java.time.Instant;

/**
 * F3 mutating flow — collections case actions, moved here from banking_web (R4): the stage
 * preconditions that used to live client-side in collections.page.ts are now enforced on the
 * backend (see docs/services/banking_backend.md). action_type values are constrained by the
 * collections_action CHECK constraint (REMINDER_SENT|SETTLEMENT_PROPOSED|EXTERNAL_TRANSFER, see
 * db/changelog/schema/008-collections.sql) — the governance audit "action" field uses the longer,
 * more descriptive names independently.
 */
@Service
@Transactional
public class CollectionsActionsService {

    private final CollectionsCaseRepository collectionsCases;
    private final CollectionsActionRepository collectionsActions;
    private final EmployeeRepository employees;
    private final GovernanceClient governanceClient;

    public CollectionsActionsService(
            CollectionsCaseRepository collectionsCases,
            CollectionsActionRepository collectionsActions,
            EmployeeRepository employees,
            GovernanceClient governanceClient) {
        this.collectionsCases = collectionsCases;
        this.collectionsActions = collectionsActions;
        this.employees = employees;
        this.governanceClient = governanceClient;
    }

    /** No stage change — only blocked once the case has reached 'legal' (judicial track). */
    public CollectionsCaseDto remind(String id, String employeeId) {
        CollectionsCase c = requireCase(id);
        if ("legal".equals(c.getStage())) {
            throw new InvalidStageTransitionException(
                    "Collections case " + id + " is already in stage 'legal'");
        }
        String auditEventId = governanceClient.recordAuditEvent(
                employeeId, "COLLECTIONS_REMINDER_SENT", "COLLECTIONS_CASE", id, null);
        recordAction(c, "REMINDER_SENT", employeeId, auditEventId);
        return toDto(c);
    }

    public CollectionsCaseDto proposeSettlement(String id, String employeeId) {
        CollectionsCase c = requireCase(id);
        String auditEventId = governanceClient.recordAuditEvent(
                employeeId, "COLLECTIONS_SETTLEMENT_PROPOSED", "COLLECTIONS_CASE", id, null);
        c.setStage("settlement");
        collectionsCases.save(c);
        recordAction(c, "SETTLEMENT_PROPOSED", employeeId, auditEventId);
        return toDto(c);
    }

    public CollectionsCaseDto transferExternal(String id, String employeeId) {
        CollectionsCase c = requireCase(id);
        if ("monitoring".equals(c.getStage()) || "reminder".equals(c.getStage())) {
            throw new InvalidStageTransitionException(
                    "Collections case " + id + " is not yet eligible for external transfer (stage: "
                            + c.getStage() + ")");
        }
        String auditEventId = governanceClient.recordAuditEvent(
                employeeId, "COLLECTIONS_TRANSFERRED_EXTERNAL", "COLLECTIONS_CASE", id, null);
        c.setStage("external");
        collectionsCases.save(c);
        recordAction(c, "EXTERNAL_TRANSFER", employeeId, auditEventId);
        return toDto(c);
    }

    private void recordAction(CollectionsCase c, String actionType, String employeeId, String auditEventId) {
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + employeeId));
        CollectionsAction action = new CollectionsAction();
        action.setCollectionsCase(c);
        action.setActionType(actionType);
        action.setPerformedAt(Instant.now());
        action.setPerformedBy(employee);
        action.setGovernanceAuditEventId(auditEventId);
        collectionsActions.save(action);
    }

    private CollectionsCase requireCase(String id) {
        return collectionsCases.findById(id)
                .orElseThrow(() -> new NotFoundException("Collections case not found: " + id));
    }

    static CollectionsCaseDto toDto(CollectionsCase c) {
        return new CollectionsCaseDto(
                c.getId(),
                c.getLoanApplication().getCustomer().getFullName(),
                c.getLoanApplication().getId(),
                c.getDaysPastDue(),
                c.getOutstandingAmount(),
                c.getStage());
    }
}
