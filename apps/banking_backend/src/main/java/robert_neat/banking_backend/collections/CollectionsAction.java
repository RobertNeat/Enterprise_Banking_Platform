package robert_neat.banking_backend.collections;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import robert_neat.banking_backend.customer.Employee;

@Entity
@Table(name = "collections_action")
public class CollectionsAction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "collections_case_id", nullable = false)
  private CollectionsCase collectionsCase;

  @Column(name = "action_type", length = 20, nullable = false)
  private String actionType;

  @Column(name = "performed_at", nullable = false)
  private Instant performedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performed_by", nullable = false)
  private Employee performedBy;

  @Column(name = "governance_audit_event_id", length = 36)
  private String governanceAuditEventId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CollectionsCase getCollectionsCase() {
    return collectionsCase;
  }

  public void setCollectionsCase(CollectionsCase collectionsCase) {
    this.collectionsCase = collectionsCase;
  }

  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
    this.actionType = actionType;
  }

  public Instant getPerformedAt() {
    return performedAt;
  }

  public void setPerformedAt(Instant performedAt) {
    this.performedAt = performedAt;
  }

  public Employee getPerformedBy() {
    return performedBy;
  }

  public void setPerformedBy(Employee performedBy) {
    this.performedBy = performedBy;
  }

  public String getGovernanceAuditEventId() {
    return governanceAuditEventId;
  }

  public void setGovernanceAuditEventId(String governanceAuditEventId) {
    this.governanceAuditEventId = governanceAuditEventId;
  }
}
