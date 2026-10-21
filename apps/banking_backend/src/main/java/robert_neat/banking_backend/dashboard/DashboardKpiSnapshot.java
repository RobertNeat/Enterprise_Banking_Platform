package robert_neat.banking_backend.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dashboard_kpi_snapshot")
public class DashboardKpiSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @Column(name = "active_loans_count", nullable = false)
  private Integer activeLoansCount;

  @Column(name = "loan_portfolio_total", precision = 19, scale = 2, nullable = false)
  private BigDecimal loanPortfolioTotal;

  @Column(name = "cards_issued_count", nullable = false)
  private Integer cardsIssuedCount;

  @Column(name = "npl_ratio", precision = 5, scale = 2, nullable = false)
  private BigDecimal nplRatio;

  @Column(name = "lcr_ratio", precision = 5, scale = 2, nullable = false)
  private BigDecimal lcrRatio;

  @Column(name = "nsfr_ratio", precision = 5, scale = 2, nullable = false)
  private BigDecimal nsfrRatio;

  @Column(name = "open_cases_count", nullable = false)
  private Integer openCasesCount;

  @Column(name = "collections_overdue_total", precision = 19, scale = 2, nullable = false)
  private BigDecimal collectionsOverdueTotal;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public LocalDate getSnapshotDate() {
    return snapshotDate;
  }

  public void setSnapshotDate(LocalDate snapshotDate) {
    this.snapshotDate = snapshotDate;
  }

  public Integer getActiveLoansCount() {
    return activeLoansCount;
  }

  public void setActiveLoansCount(Integer activeLoansCount) {
    this.activeLoansCount = activeLoansCount;
  }

  public BigDecimal getLoanPortfolioTotal() {
    return loanPortfolioTotal;
  }

  public void setLoanPortfolioTotal(BigDecimal loanPortfolioTotal) {
    this.loanPortfolioTotal = loanPortfolioTotal;
  }

  public Integer getCardsIssuedCount() {
    return cardsIssuedCount;
  }

  public void setCardsIssuedCount(Integer cardsIssuedCount) {
    this.cardsIssuedCount = cardsIssuedCount;
  }

  public BigDecimal getNplRatio() {
    return nplRatio;
  }

  public void setNplRatio(BigDecimal nplRatio) {
    this.nplRatio = nplRatio;
  }

  public BigDecimal getLcrRatio() {
    return lcrRatio;
  }

  public void setLcrRatio(BigDecimal lcrRatio) {
    this.lcrRatio = lcrRatio;
  }

  public BigDecimal getNsfrRatio() {
    return nsfrRatio;
  }

  public void setNsfrRatio(BigDecimal nsfrRatio) {
    this.nsfrRatio = nsfrRatio;
  }

  public Integer getOpenCasesCount() {
    return openCasesCount;
  }

  public void setOpenCasesCount(Integer openCasesCount) {
    this.openCasesCount = openCasesCount;
  }

  public BigDecimal getCollectionsOverdueTotal() {
    return collectionsOverdueTotal;
  }

  public void setCollectionsOverdueTotal(BigDecimal collectionsOverdueTotal) {
    this.collectionsOverdueTotal = collectionsOverdueTotal;
  }
}
