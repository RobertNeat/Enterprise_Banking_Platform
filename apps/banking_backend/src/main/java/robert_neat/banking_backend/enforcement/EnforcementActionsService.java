package robert_neat.banking_backend.enforcement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.ApprovalRecord;
import robert_neat.banking_backend.approvals.ApprovalsService;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.InvalidRequestException;
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
 * gatewayEnforcementId linkage: newly registered cases get a real gatewayEnforcementId from the
 * gateway's response. Seeded cases (see db/changelog/seed/009-enforcement.sql) are linked to
 * external_institutions_gateway_mock's own fixed-UUID seed (see EnforcementService.seed() there),
 * so actions on seeded cases resolve against the mock the same way as on newly registered ones.
 * transfer()/requestTransferApproval() still reject with 409 if gatewayEnforcementId is ever null
 * (defensive — should not happen with the current seed, but guards any future unlinked case).
 */
@Service
@Transactional
public class EnforcementActionsService {

    private static final Logger log = LoggerFactory.getLogger(EnforcementActionsService.class);

    private final EnforcementCaseRepository enforcementCases;
    private final CustomerRepository customers;
    private final RegulatoryGatewayClient gatewayClient;
    private final GovernanceClient governanceClient;
    private final ApprovalsService approvalsService;
    private final EnforcementIdGenerator idGenerator;
    private final BigDecimal nonAlimonyProtectedAmountLimit;
    private final BigDecimal alimonyProtectedAmountLimit;

    public EnforcementActionsService(
            EnforcementCaseRepository enforcementCases,
            CustomerRepository customers,
            RegulatoryGatewayClient gatewayClient,
            GovernanceClient governanceClient,
            ApprovalsService approvalsService,
            EnforcementIdGenerator idGenerator,
            @Value("${banking.enforcement.protected-amount-limit.non-alimony:3604.50}")
            BigDecimal nonAlimonyProtectedAmountLimit,
            @Value("${banking.enforcement.protected-amount-limit.alimony:0.00}")
            BigDecimal alimonyProtectedAmountLimit) {
        this.enforcementCases = enforcementCases;
        this.customers = customers;
        this.gatewayClient = gatewayClient;
        this.governanceClient = governanceClient;
        this.approvalsService = approvalsService;
        this.idGenerator = idGenerator;
        this.nonAlimonyProtectedAmountLimit = nonAlimonyProtectedAmountLimit;
        this.alimonyProtectedAmountLimit = alimonyProtectedAmountLimit;
    }

    public EnforcementCaseDto register(RegisterEnforcementRequest request, String employeeId) {
        Customer customer = customers.findById(request.customer())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customer()));

        // kind defaults to NON_ALIMONY when omitted, so pre-ZJC-03 callers that never sent it keep working.
        String kind = request.kind() != null ? request.kind() : "NON_ALIMONY";
        BigDecimal protectedAmount = request.protectedAmount() != null ? request.protectedAmount() : BigDecimal.ZERO;
        validateKindAndProtectedAmount(kind, protectedAmount);
        if (request.blockedAmount() == null || request.blockedAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("blockedAmount must be zero or positive");
        }

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
        enforcementCase.setKind(kind);
        enforcementCase.setBlockedAmount(request.blockedAmount());
        enforcementCase.setProtectedAmount(protectedAmount);
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
        approvalsService.requireCheckerExists(employeeId);

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

    /**
     * ZJC-03 — "kwota wolna" (protected-amount) ceiling per garnishment kind. NON_ALIMONY mirrors
     * art. 54 ust. 1 Prawa bankowego: 75% of the 2026 statutory minimum gross wage (4806 PLN per
     * Rozporządzenie RM z 11.09.2025, Dz.U. 2025 poz. 1242) = 3604.50 PLN. ALIMONY defaults to 0.00:
     * alimony enforcement is not covered by the art. 54 bank-account exemption, so no protected
     * amount applies here by default. Both are simulation defaults for this demo, not binding legal
     * advice — override via banking.enforcement.protected-amount-limit.{non-alimony,alimony}.
     */
    private void validateKindAndProtectedAmount(String kind, BigDecimal protectedAmount) {
        BigDecimal limit = switch (kind) {
            case "NON_ALIMONY" -> nonAlimonyProtectedAmountLimit;
            case "ALIMONY" -> alimonyProtectedAmountLimit;
            default -> throw new InvalidRequestException("kind must be one of ALIMONY, NON_ALIMONY");
        };
        if (protectedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("protectedAmount must not be negative");
        }
        if (protectedAmount.compareTo(limit) > 0) {
            throw new InvalidRequestException(
                    "protectedAmount " + protectedAmount + " exceeds the " + kind
                            + " protected-amount limit (" + limit + ")");
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
