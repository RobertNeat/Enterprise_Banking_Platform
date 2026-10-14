package robert_neat.banking_backend.loan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.common.BankTimeZone;
import robert_neat.banking_backend.common.EmployeeNames;
import robert_neat.banking_backend.common.NotFoundException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LoanService {

    private final LoanApplicationRepository loanApplications;
    private final InstallmentRepository installments;
    private final LoanStageTransitionRepository stageTransitions;

    public LoanService(
            LoanApplicationRepository loanApplications,
            InstallmentRepository installments,
            LoanStageTransitionRepository stageTransitions) {
        this.loanApplications = loanApplications;
        this.installments = installments;
        this.stageTransitions = stageTransitions;
    }

    public List<LoanSummaryDto> findAll() {
        return loanApplications.findAll().stream().map(LoanService::toSummary).toList();
    }

    public LoanDetailDto findById(String id) {
        LoanApplication loan = loanApplications.findById(id)
                .orElseThrow(() -> new NotFoundException("Loan application not found: " + id));
        List<InstallmentDto> installmentDtos = installments
                .findByLoanApplicationIdOrderByInstallmentNoAsc(id)
                .stream()
                .map(LoanService::toInstallmentDto)
                .toList();
        List<LoanStageTransitionDto> transitionDtos = stageTransitions
                .findByLoanApplicationIdOrderByTransitionedAtAsc(id)
                .stream()
                .map(LoanService::toTransitionDto)
                .toList();
        return new LoanDetailDto(
                loan.getId(),
                loan.getCustomer().getFullName(),
                loan.getSegment(),
                loan.getProduct(),
                loan.getStage(),
                loan.getRequestedAmount(),
                loan.getApprovedAmount(),
                loan.getScoreResult() == null ? 0 : loan.getScoreResult(),
                loan.getDsti() == null ? java.math.BigDecimal.ZERO : loan.getDsti(),
                loan.getCreatedAt().atZone(BankTimeZone.ZONE).toLocalDate(),
                loan.getInterestRate(),
                loan.getTermMonths(),
                installmentDtos,
                transitionDtos);
    }

    private static LoanSummaryDto toSummary(LoanApplication loan) {
        return new LoanSummaryDto(
                loan.getId(),
                loan.getCustomer().getFullName(),
                loan.getSegment(),
                loan.getProduct(),
                loan.getStage(),
                loan.getRequestedAmount(),
                loan.getApprovedAmount(),
                loan.getScoreResult() == null ? 0 : loan.getScoreResult(),
                loan.getDsti() == null ? java.math.BigDecimal.ZERO : loan.getDsti(),
                loan.getCreatedAt().atZone(BankTimeZone.ZONE).toLocalDate());
    }

    private static InstallmentDto toInstallmentDto(Installment installment) {
        return new InstallmentDto(
                installment.getInstallmentNo(),
                installment.getDueDate(),
                installment.getPrincipal(),
                installment.getInterest(),
                installment.getStatus());
    }

    private static LoanStageTransitionDto toTransitionDto(LoanStageTransition transition) {
        return new LoanStageTransitionDto(
                transition.getFromStage(),
                transition.getToStage(),
                transition.getTransitionedAt(),
                EmployeeNames.abbreviated(transition.getTransitionedBy()),
                transition.getApprovalId());
    }
}
