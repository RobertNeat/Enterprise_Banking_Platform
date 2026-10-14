package robert_neat.banking_backend.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import robert_neat.banking_backend.customer.Employee;

import java.time.Instant;

@Entity
@Table(name = "loan_stage_transition")
public class LoanStageTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "from_stage", length = 20, nullable = false)
    private String fromStage;

    @Column(name = "to_stage", length = 20, nullable = false)
    private String toStage;

    @Column(name = "approval_id", length = 36)
    private String approvalId;

    @Column(name = "transitioned_at", nullable = false)
    private Instant transitionedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transitioned_by", nullable = false)
    private Employee transitionedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LoanApplication getLoanApplication() {
        return loanApplication;
    }

    public void setLoanApplication(LoanApplication loanApplication) {
        this.loanApplication = loanApplication;
    }

    public String getFromStage() {
        return fromStage;
    }

    public void setFromStage(String fromStage) {
        this.fromStage = fromStage;
    }

    public String getToStage() {
        return toStage;
    }

    public void setToStage(String toStage) {
        this.toStage = toStage;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public Instant getTransitionedAt() {
        return transitionedAt;
    }

    public void setTransitionedAt(Instant transitionedAt) {
        this.transitionedAt = transitionedAt;
    }

    public Employee getTransitionedBy() {
        return transitionedBy;
    }

    public void setTransitionedBy(Employee transitionedBy) {
        this.transitionedBy = transitionedBy;
    }
}
