package robert_neat.banking_backend.loan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.ApprovalRecord;
import robert_neat.banking_backend.approvals.ApprovalsService;
import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.InvalidRequestException;
import robert_neat.banking_backend.common.InvalidStageTransitionException;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.creditrisk.CreditRiskClient;
import robert_neat.banking_backend.creditrisk.CreditScoreResult;
import robert_neat.banking_backend.customer.Customer;
import robert_neat.banking_backend.customer.CustomerRepository;
import robert_neat.banking_backend.customer.Employee;
import robert_neat.banking_backend.customer.EmployeeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * F2 mutating flow — submission (scoring via credit_risk_mock), submit-for-approval (governance
 * maker-checker queue), advance-stage (contract + installment generation, or back to 'application'
 * on a REJECTED decision), and disburse (contract -> disbursement -> schedule).
 *
 * Design decision (R2): advance-stage is a separate endpoint the employee calls manually after
 * seeing the approval decided APPROVED, rather than ApprovalsService.decide() calling back into
 * this service. Reason: ApprovalRecord only carries a free-text payload (see
 * approvals/ApprovalRecord.java) — ApprovalsService has no structured knowledge of "this is a
 * loan, advance it" and would need to parse that text to find the loan id. Storing the
 * approvalId on the LoanStageTransition row (column already existed, unused until now) and
 * having advance-stage read it back via GovernanceClient.getApproval() keeps approvals/ and
 * loan/ decoupled, matching the existing UI pattern where employees click actions explicitly
 * (see loan-detail.page.ts's advanceStage()).
 */
@Service
@Transactional
public class LoanActionsService {

    /** No per-product rate table exists yet (seed uses a flat 7.9% for every product) — reused here. */
    static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.0790");
    static final int DEFAULT_TERM_MONTHS = 36;

    /** Must mirror the CHECK constraint on loan_application.segment (db/changelog/schema/002-loan.sql). */
    private static final List<String> VALID_SEGMENTS = List.of("retail", "business");
    /** Must mirror the CHECK constraint on loan_application.product (db/changelog/schema/002-loan.sql). */
    private static final List<String> VALID_PRODUCTS =
            List.of("cash", "mortgage", "overdraft", "investment", "creditLine");

    private final LoanApplicationRepository loanApplications;
    private final LoanStageTransitionRepository stageTransitions;
    private final InstallmentRepository installments;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final CreditRiskClient creditRiskClient;
    private final GovernanceClient governanceClient;
    private final ApprovalsService approvalsService;
    private final LoanIdGenerator idGenerator;
    private final LoanService loanService;

    public LoanActionsService(
            LoanApplicationRepository loanApplications,
            LoanStageTransitionRepository stageTransitions,
            InstallmentRepository installments,
            CustomerRepository customers,
            EmployeeRepository employees,
            CreditRiskClient creditRiskClient,
            GovernanceClient governanceClient,
            ApprovalsService approvalsService,
            LoanIdGenerator idGenerator,
            LoanService loanService) {
        this.loanApplications = loanApplications;
        this.stageTransitions = stageTransitions;
        this.installments = installments;
        this.customers = customers;
        this.employees = employees;
        this.creditRiskClient = creditRiskClient;
        this.governanceClient = governanceClient;
        this.approvalsService = approvalsService;
        this.idGenerator = idGenerator;
        this.loanService = loanService;
    }

    public LoanDetailDto create(CreateLoanApplicationRequest request) {
        validateCreateRequest(request);

        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));

        // Scoring call happens before any DB write — a credit_risk_mock outage must leave no row
        // (see docs/services/banking_backend.md, F2 section).
        CreditScoreResult score = creditRiskClient.score(
                customer.getId(), request.requestedAmount(), request.segment());

        LoanApplication loan = new LoanApplication();
        loan.setId(idGenerator.next());
        loan.setCustomer(customer);
        loan.setSegment(request.segment());
        loan.setProduct(request.product());
        loan.setStage("application");
        loan.setRequestedAmount(request.requestedAmount());
        loan.setScoreResult(score.scoreResult());
        loan.setDsti(score.dsti());
        loan.setBureauInquiryId(score.inquiryId());
        loan.setInterestRate(DEFAULT_INTEREST_RATE);
        loan.setTermMonths(request.termMonths() != null ? request.termMonths() : DEFAULT_TERM_MONTHS);
        loan.setCreatedAt(Instant.now());
        loanApplications.save(loan);

        return loanService.findById(loan.getId());
    }

    /**
     * Must run before any side effect (credit_risk_mock scoring, DB write) — an invalid
     * segment/product would otherwise still burn a bureau inquiry and/or surface as a raw
     * DataIntegrityViolationException (500) when the DB's CHECK constraint rejects the insert.
     */
    private void validateCreateRequest(CreateLoanApplicationRequest request) {
        if (request.segment() == null || !VALID_SEGMENTS.contains(request.segment())) {
            throw new InvalidRequestException(
                    "segment must be one of: " + String.join(", ", VALID_SEGMENTS));
        }
        if (request.product() == null || !VALID_PRODUCTS.contains(request.product())) {
            throw new InvalidRequestException(
                    "product must be one of: " + String.join(", ", VALID_PRODUCTS));
        }
    }

    public SubmitForApprovalResponse submitForApproval(String loanId, String employeeId) {
        LoanApplication loan = loanApplications.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan application not found: " + loanId));
        if (!"application".equals(loan.getStage())) {
            throw new InvalidStageTransitionException(
                    "Loan " + loanId + " is not in stage 'application' (current: " + loan.getStage() + ")");
        }
        Employee employee = requireEmployee(employeeId);
        approvalsService.requireCheckerExists(employeeId);

        String fromStage = loan.getStage();
        ApprovalRecord approval = governanceClient.createApproval(
                "LOAN_STAGE_TRANSITION", employeeId,
                "Loan " + loanId + " requested amount " + loan.getRequestedAmount());

        loan.setStage("decision");
        loanApplications.save(loan);
        recordTransition(loan, fromStage, "decision", approval.id(), employee);
        governanceClient.recordAuditEvent(employeeId, "LOAN_STAGE_CHANGED", "LOAN_APPLICATION", loanId,
                fromStage + " -> decision");

        return new SubmitForApprovalResponse(loanService.findById(loanId), approval.id());
    }

    public LoanDetailDto advanceStage(String loanId, String employeeId) {
        LoanApplication loan = loanApplications.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan application not found: " + loanId));
        if (!"decision".equals(loan.getStage())) {
            throw new InvalidStageTransitionException(
                    "Loan " + loanId + " is not in stage 'decision' (current: " + loan.getStage() + ")");
        }
        Employee employee = requireEmployee(employeeId);

        LoanStageTransition lastTransition = stageTransitions
                .findByLoanApplicationIdOrderByTransitionedAtAsc(loanId).stream()
                .filter(t -> "decision".equals(t.getToStage()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new InvalidStageTransitionException(
                        "Loan " + loanId + " has no recorded approval request"));
        String approvalId = lastTransition.getApprovalId();
        if (approvalId == null) {
            throw new InvalidStageTransitionException("Loan " + loanId + " has no linked approval");
        }
        ApprovalRecord approval = governanceClient.getApproval(approvalId);

        // REJECTED is not a dead end: the applicant should be able to fix and resubmit, mirroring
        // ReportingActionsService's REJECTED -> 'draft' revert (see its class javadoc). Unlike a
        // still-PENDING approval (which legitimately stays a 409 until decided), REJECTED is a
        // final decision, so this branch must not throw — it returns 200 with the loan back at
        // 'application'.
        if ("REJECTED".equals(approval.status())) {
            String fromStage = loan.getStage();
            loan.setStage("application");
            loanApplications.save(loan);
            recordTransition(loan, fromStage, "application", approvalId, employee);
            governanceClient.recordAuditEvent(employeeId, "LOAN_STAGE_CHANGED", "LOAN_APPLICATION", loanId,
                    fromStage + " -> application");
            return loanService.findById(loanId);
        }
        if (!"APPROVED".equals(approval.status())) {
            throw new InvalidStageTransitionException(
                    "Approval " + approvalId + " is not APPROVED (status: " + approval.status() + ")");
        }

        String fromStage = loan.getStage();
        loan.setStage("contract");
        loan.setApprovedAmount(loan.getRequestedAmount());
        loanApplications.save(loan);
        recordTransition(loan, fromStage, "contract", approvalId, employee);
        generateInstallments(loan);
        governanceClient.recordAuditEvent(employeeId, "LOAN_STAGE_CHANGED", "LOAN_APPLICATION", loanId,
                fromStage + " -> contract");

        return loanService.findById(loanId);
    }

    /**
     * F2 — contract -> disbursement -> schedule. Both CHECK-constraint-valid stages are reachable
     * only through this explicit employee action (no governance approval gate here — disbursement
     * is an operational step after the contract stage's approval already happened in advanceStage).
     * Two stage_transition rows + two LOAN_STAGE_CHANGED audits, one per hop, so the audit trail and
     * timeline read the same way submitForApproval/advanceStage's single-hop audits do. The second
     * transition's timestamp is nudged forward by 1ms so the frontend's transitionedAt sort (see
     * loan-detail.page.ts's stageTransitions) orders the two hops deterministically.
     */
    public LoanDetailDto disburse(String loanId, String employeeId) {
        LoanApplication loan = loanApplications.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan application not found: " + loanId));
        if (!"contract".equals(loan.getStage())) {
            throw new InvalidStageTransitionException(
                    "Loan " + loanId + " is not in stage 'contract' (current: " + loan.getStage() + ")");
        }
        Employee employee = requireEmployee(employeeId);

        Instant firstHop = Instant.now();
        loan.setStage("disbursement");
        loanApplications.save(loan);
        recordTransition(loan, "contract", "disbursement", null, employee, firstHop);
        governanceClient.recordAuditEvent(employeeId, "LOAN_STAGE_CHANGED", "LOAN_APPLICATION", loanId,
                "contract -> disbursement");

        loan.setStage("schedule");
        loanApplications.save(loan);
        recordTransition(loan, "disbursement", "schedule", null, employee, firstHop.plusMillis(1));
        governanceClient.recordAuditEvent(employeeId, "LOAN_STAGE_CHANGED", "LOAN_APPLICATION", loanId,
                "disbursement -> schedule");

        return loanService.findById(loanId);
    }

    /** Simple equal-installment annuity — not a production-grade amortization engine, just fills the UI. */
    private void generateInstallments(LoanApplication loan) {
        BigDecimal amount = loan.getApprovedAmount();
        int term = loan.getTermMonths();
        BigDecimal monthlyRate = loan.getInterestRate().divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal payment;
        if (monthlyRate.signum() == 0) {
            payment = amount.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
            BigDecimal factor = onePlusR.pow(term);
            payment = amount.multiply(monthlyRate).multiply(factor)
                    .divide(factor.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        List<Installment> rows = new ArrayList<>();
        BigDecimal remainingPrincipal = amount;
        LocalDate dueDate = LocalDate.now(robert_neat.banking_backend.common.BankTimeZone.ZONE);
        for (int no = 1; no <= term; no++) {
            dueDate = dueDate.plusMonths(1);
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal;
            if (no == term) {
                principal = remainingPrincipal; // last row absorbs rounding so principals sum to amount
            } else {
                principal = payment.subtract(interest).setScale(2, RoundingMode.HALF_UP);
            }
            remainingPrincipal = remainingPrincipal.subtract(principal);

            Installment installment = new Installment();
            installment.setLoanApplication(loan);
            installment.setInstallmentNo(no);
            installment.setDueDate(dueDate);
            installment.setPrincipal(principal);
            installment.setInterest(interest);
            installment.setStatus("upcoming");
            rows.add(installment);
        }
        installments.saveAll(rows);
    }

    private void recordTransition(
            LoanApplication loan, String fromStage, String toStage, String approvalId, Employee employee) {
        recordTransition(loan, fromStage, toStage, approvalId, employee, Instant.now());
    }

    private void recordTransition(
            LoanApplication loan, String fromStage, String toStage, String approvalId, Employee employee,
            Instant transitionedAt) {
        LoanStageTransition transition = new LoanStageTransition();
        transition.setLoanApplication(loan);
        transition.setFromStage(fromStage);
        transition.setToStage(toStage);
        transition.setApprovalId(approvalId);
        transition.setTransitionedAt(transitionedAt);
        transition.setTransitionedBy(employee);
        stageTransitions.save(transition);
    }

    private Employee requireEmployee(String employeeId) {
        return employees.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + employeeId));
    }
}
