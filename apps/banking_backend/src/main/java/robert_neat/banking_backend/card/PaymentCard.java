package robert_neat.banking_backend.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import robert_neat.banking_backend.customer.Customer;

@Entity
@Table(name = "payment_card")
public class PaymentCard {

  @Id
  @Column(name = "id", length = 10)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "masked_number", length = 19, nullable = false)
  private String maskedNumber;

  @Column(name = "network", length = 12, nullable = false)
  private String network;

  @Column(name = "product", length = 50, nullable = false)
  private String product;

  @Column(name = "status", length = 10, nullable = false)
  private String status;

  @Column(name = "expiry", length = 7, nullable = false)
  private String expiry;

  @Column(name = "limit_daily", precision = 19, scale = 2, nullable = false)
  private BigDecimal limitDaily;

  @Column(name = "limit_atm", precision = 19, scale = 2, nullable = false)
  private BigDecimal limitAtm;

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

  public String getMaskedNumber() {
    return maskedNumber;
  }

  public void setMaskedNumber(String maskedNumber) {
    this.maskedNumber = maskedNumber;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getProduct() {
    return product;
  }

  public void setProduct(String product) {
    this.product = product;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getExpiry() {
    return expiry;
  }

  public void setExpiry(String expiry) {
    this.expiry = expiry;
  }

  public BigDecimal getLimitDaily() {
    return limitDaily;
  }

  public void setLimitDaily(BigDecimal limitDaily) {
    this.limitDaily = limitDaily;
  }

  public BigDecimal getLimitAtm() {
    return limitAtm;
  }

  public void setLimitAtm(BigDecimal limitAtm) {
    this.limitAtm = limitAtm;
  }
}
