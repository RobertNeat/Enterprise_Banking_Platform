package robert_neat.banking_backend.collections;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import robert_neat.banking_backend.loan.LoanApplication;

@Entity
@Table(name = "collections_case")
public class CollectionsCase {

  @Id
  @Column(name = "id", length = 10)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loan_application_id", nullable = false)
  private LoanApplication loanApplication;

  @Column(name = "days_past_due", nullable = false)
  private Integer daysPastDue;

  @Column(name = "outstanding_amount", precision = 19, scale = 2, nullable = false)
  private BigDecimal outstandingAmount;

  @Column(name = "stage", length = 20, nullable = false)
  private String stage;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LoanApplication getLoanApplication() {
    return loanApplication;
  }

  public void setLoanApplication(LoanApplication loanApplication) {
    this.loanApplication = loanApplication;
  }

  public Integer getDaysPastDue() {
    return daysPastDue;
  }

  public void setDaysPastDue(Integer daysPastDue) {
    this.daysPastDue = daysPastDue;
  }

  public BigDecimal getOutstandingAmount() {
    return outstandingAmount;
  }

  public void setOutstandingAmount(BigDecimal outstandingAmount) {
    this.outstandingAmount = outstandingAmount;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }
}
