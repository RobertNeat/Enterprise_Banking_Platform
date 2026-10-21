package robert_neat.banking_backend.risk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import robert_neat.banking_backend.customer.Customer;

@Entity
@Table(name = "collateral_entry")
public class CollateralEntry {

  @Id
  @Column(name = "id", length = 10)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "type", length = 20, nullable = false)
  private String type;

  @Column(name = "valuation", precision = 19, scale = 2, nullable = false)
  private BigDecimal valuation;

  @Column(name = "ltv", precision = 5, scale = 2, nullable = false)
  private BigDecimal ltv;

  @Column(name = "rating", length = 10, nullable = false)
  private String rating;

  @Column(name = "stage", length = 10, nullable = false)
  private String stage;

  @Column(name = "provision", precision = 19, scale = 2, nullable = false)
  private BigDecimal provision;

  @Column(name = "ecl_classified_at")
  private Instant eclClassifiedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigDecimal getValuation() {
    return valuation;
  }

  public void setValuation(BigDecimal valuation) {
    this.valuation = valuation;
  }

  public BigDecimal getLtv() {
    return ltv;
  }

  public void setLtv(BigDecimal ltv) {
    this.ltv = ltv;
  }

  public String getRating() {
    return rating;
  }

  public void setRating(String rating) {
    this.rating = rating;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }

  public BigDecimal getProvision() {
    return provision;
  }

  public void setProvision(BigDecimal provision) {
    this.provision = provision;
  }

  public Instant getEclClassifiedAt() {
    return eclClassifiedAt;
  }

  public void setEclClassifiedAt(Instant eclClassifiedAt) {
    this.eclClassifiedAt = eclClassifiedAt;
  }
}
