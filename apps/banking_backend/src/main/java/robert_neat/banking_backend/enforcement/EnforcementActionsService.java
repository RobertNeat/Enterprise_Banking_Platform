package robert_neat.banking_backend.enforcement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.ApprovalRecord;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.customer.Customer;
import robert_neat.banking_backend.customer.CustomerRepository;
import robert_neat.banking_backend.reporting.EnforcementIncomingResponse;
import robert_neat.banking_backend.reporting.EnforcementTransferResponse;
import robert_neat.banking_backend.reporting.RegulatoryGatewayClient;
import robert_neat.banking_backend.reporting.RegulatoryGatewayUnavailableException;

import java.math.BigDecimal;

/**
 * F6 mutating flow — enforcement case registration, maker-checker approval request, and funds
 * transfer, all proxied through external_institutions_gateway_mock (see
 * reporting/RegulatoryGatewayClient.java — shared with F7). Follows two established patterns:
 * - ICT incident on gateway unavailability, mirroring CardActionsService.callProcessor() (F4).
 * - Approval-gated transition, mirroring LoanActionsService.advanceStage() (F2): transfer requires
 *   the linked governance_mock approval to already be APPROVED.
 *
 * gatewayEnforcementId linkage (R1.5 open point, closed here): newly registered cases get a real
 * gatewayEnforcementId from the gateway's response. Seeded cases (see
 * db/changelog/seed/009-enforcement.sql) keep gatewayEnforcementId=NULL — they predate this round
 * and have no corresponding gateway record (the gateway only seeds its own fictitious registry,
 * unrelated to banking_backend's seed ids). transfer() rejects with 409 when the linkage is
 * missing instead of calling the gateway with a null id — a permanent demo limitation, not a TODO.
 */
@Service
@Transactional
public class EnforcementActionsService {

    private static final Logger log = LoggerFactory.getLogger(EnforcementActionsService.class);

    private final EnforcementCaseRepository enforcementCases;
    private final CustomerRepository customers;
    private final RegulatoryGatewayClient gatewayClient;
    private final GovernanceClient governanceClient;
    private final EnforcementIdGenerator idGenerator;

    public EnforcementActionsService(
            EnforcementCaseRepository enforcementCases,
            CustomerRepository customers,
            RegulatoryGatewayClient gatewayClient,
            GovernanceClient governanceClient,
            EnforcementIdGenerator idGenerator) {
        this.enforcementCases = enforcementCases;
        this.customers = customers;
        this.gatewayClient = gatewayClient;
        this.governanceClient = governanceClient;
        this.idGenerator = idGenerator;
    }

    public EnforcementCaseDto register(RegisterEnforcementRequest request, String employeeId) {
        Customer customer = customers.findById(request.customer())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customer()));

        EnforcementIncomingResponse gatewayResponse = callGateway("register", null, () ->
                gatewayClient.registerEnforcement(
                        request.bailiff(), request.caseNumber(), request.customer(),
                        request.blockedAmount(), request.protectedAmount()));

        EnforcementCase enforcementCase = new EnforcementCase();
        enforcementCase.setId(idGenerator.next());
        enforcementCase.setGatewayEnforcementId(gatewayResponse.id());
        enforcementCase.setCustomer(customer);
        enforcementCase.setBailiff(request.bailiff());
        enforcementCase.setCaseNumber(request.caseNumber());
        enforcementCase.setKind("NON_ALIMONY"); // mock data carries no alimony signal (see seed comment)
        enforcementCase.setBlockedAmount(request.blockedAmount());
        enforcementCase.setProtectedAmount(
                request.protectedAmount() != null ? request.protectedAmount() : BigDecimal.ZERO);
        enforcementCase.setStatus("registered");
        enforcementCases.save(enforcementCase);

        governanceClient.recordAuditEvent(employeeId, "ENFORCEMENT_REGISTERED", "ENFORCEMENT_CASE",
                enforcementCase.getId(), "blockedAmount=" + request.blockedAmount());

        return toDto(enforcementCase);
    }

    public RequestTransferApprovalResponse requestTransferApproval(String id, String employeeId) {
        EnforcementCase enforcementCase = requireCase(id);
        if (!"registered".equals(enforcementCase.getStatus())) {
            throw new InvalidStageTransitionException(
                    "Enforcement case " + id + " is not in status 'registered' (current: "
                            + enforcementCase.getStatus() + ")");
        }
        if (enforcementCase.getGatewayEnforcementId() == null) {
            throw new InvalidStageTransitionException(
                    "Enforcement case " + id + " has no gateway linkage (seeded case) — cannot request a transfer");
        }

        ApprovalRecord approval = governanceClient.createApproval(
                "ENFORCEMENT_TRANSFER", employeeId,
                "Enforcement case " + id + " blocked amount " + enforcementCase.getBlockedAmount());

        enforcementCase.setApprovalId(approval.id());
        enforcementCase.setStatus("active");
        enforcementCases.save(enforcementCase);

        governanceClient.recordAuditEvent(employeeId, "ENFORCEMENT_TRANSFER_REQUESTED", "ENFORCEMENT_CASE",
                id, "approvalId=" + approval.id());

        return new RequestTransferApprovalResponse(toDto(enforcementCase), approval.id());
    }

    public EnforcementCaseDto transfer(String id, TransferEnforcementRequest request, String employeeId) {
        EnforcementCase enforcementCase = requireCase(id);
        if (!"active".equals(enforcementCase.getStatus())) {
            throw new InvalidStageTransitionException(
                    "Enforcement case " + id + " is not in status 'active' (current: "
                            + enforcementCase.getStatus() + ")");
        }
        if (enforcementCase.getGatewayEnforcementId() == null) {
            throw new InvalidStageTransitionException(
                    "Enforcement case " + id + " has no gateway linkage (seeded case) — cannot transfer funds");
        }
        String approvalId = enforcementCase.getApprovalId();
        if (approvalId == null) {
            throw new InvalidStageTransitionException("Enforcement case " + id + " has no linked approval");
        }
        ApprovalRecord approval = governanceClient.getApproval(approvalId);
        if (!"APPROVED".equals(approval.status())) {
            throw new InvalidStageTransitionException(
                    "Approval " + approvalId + " is not APPROVED (status: " + approval.status() + ")");
        }

        EnforcementTransferResponse gatewayResponse = callGateway("transfer", id, () ->
                gatewayClient.transferEnforcement(
                        enforcementCase.getGatewayEnforcementId(), request.transferredAmount()));

        enforcementCase.setStatus("settled");
        enforcementCase.setTransferredAmount(request.transferredAmount());
        enforcementCases.save(enforcementCase);

        governanceClient.recordAuditEvent(employeeId, "ENFORCEMENT_FUNDS_TRANSFERRED", "ENFORCEMENT_CASE",
                id, "transferredAmount=" + request.transferredAmount() + " gatewayStatus=" + gatewayResponse.status());

        return toDto(enforcementCase);
    }

    /** Mirrors CardActionsService.callProcessor() — gateway outage reported as an ICT incident before rethrow. */
    private <T> T callGateway(String operation, String caseId, java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RegulatoryGatewayUnavailableException ex) {
            reportIctIncident(operation, caseId, ex);
            throw ex;
        }
    }

    private void reportIctIncident(String operation, String caseId, RegulatoryGatewayUnavailableException cause) {
        try {
            governanceClient.createIctIncident(
                    "EXTERNAL_INSTITUTIONS_GATEWAY",
                    "external_institutions_gateway_mock unavailable during " + operation
                            + (caseId != null ? " for case " + caseId : "") + ": " + cause.getMessage(),
                    "HIGH");
        } catch (RuntimeException ictFailure) {
            log.warn("Failed to record ICT incident for gateway outage ({}): {}", operation, ictFailure.getMessage());
        }
    }

    private EnforcementCase requireCase(String id) {
        return enforcementCases.findById(id)
                .orElseThrow(() -> new NotFoundException("Enforcement case not found: " + id));
    }

    static EnforcementCaseDto toDto(EnforcementCase e) {
        return new EnforcementCaseDto(
                e.getId(),
                e.getCustomer().getFullName(),
                e.getBailiff(),
                e.getCaseNumber(),
                e.getBlockedAmount(),
                e.getTransferredAmount() == null ? BigDecimal.ZERO : e.getTransferredAmount(),
                e.getProtectedAmount(),
                e.getStatus());
    }
}
