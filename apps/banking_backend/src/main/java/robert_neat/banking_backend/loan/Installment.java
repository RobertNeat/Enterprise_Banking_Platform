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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installment")
public class Installment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loan_application_id", nullable = false)
  private LoanApplication loanApplication;

  @Column(name = "installment_no", nullable = false)
  private Integer installmentNo;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "principal", precision = 19, scale = 2, nullable = false)
  private BigDecimal principal;

  @Column(name = "interest", precision = 19, scale = 2, nullable = false)
  private BigDecimal interest;

  @Column(name = "status", length = 10, nullable = false)
  private String status;

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

  public Integer getInstallmentNo() {
    return installmentNo;
  }

  public void setInstallmentNo(Integer installmentNo) {
    this.installmentNo = installmentNo;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public BigDecimal getPrincipal() {
    return principal;
  }

  public void setPrincipal(BigDecimal principal) {
    this.principal = principal;
  }

  public BigDecimal getInterest() {
    return interest;
  }

  public void setInterest(BigDecimal interest) {
    this.interest = interest;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
