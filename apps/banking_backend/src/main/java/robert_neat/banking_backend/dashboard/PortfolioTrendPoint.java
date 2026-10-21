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
@Table(name = "portfolio_trend_point")
public class PortfolioTrendPoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @Column(name = "portfolio_total", precision = 19, scale = 2, nullable = false)
  private BigDecimal portfolioTotal;

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

  public BigDecimal getPortfolioTotal() {
    return portfolioTotal;
  }

  public void setPortfolioTotal(BigDecimal portfolioTotal) {
    this.portfolioTotal = portfolioTotal;
  }
}
