package robert_neat.banking_backend.contactcenter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import robert_neat.banking_backend.customer.Customer;

@Entity
@Table(name = "contact_history_entry")
public class ContactHistoryEntry {

  @Id
  @Column(name = "id", length = 10)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "channel_key", length = 30, nullable = false)
  private String channelKey;

  @Column(name = "occurred_at", nullable = false)
  private LocalDate occurredAt;

  @Column(name = "summary", length = 500, nullable = false)
  private String summary;

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

  public String getChannelKey() {
    return channelKey;
  }

  public void setChannelKey(String channelKey) {
    this.channelKey = channelKey;
  }

  public LocalDate getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDate occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
