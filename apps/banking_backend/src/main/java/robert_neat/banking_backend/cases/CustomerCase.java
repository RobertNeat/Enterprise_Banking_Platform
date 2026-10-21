package robert_neat.banking_backend.cases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import robert_neat.banking_backend.customer.Customer;
import robert_neat.banking_backend.customer.Employee;

@Entity
@Table(name = "customer_case")
public class CustomerCase {

  @Id
  @Column(name = "id", length = 10)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "subject", length = 300, nullable = false)
  private String subject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Employee owner;

  @Column(name = "priority", length = 10, nullable = false)
  private String priority;

  @Column(name = "case_type", length = 30, nullable = false)
  private String caseType;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @Column(name = "statutory_deadline", nullable = false)
  private LocalDate statutoryDeadline;

  @Column(name = "sla_due_at", nullable = false)
  private Instant slaDueAt;

  @Column(name = "status", length = 15, nullable = false)
  private String status;

  @Column(name = "escalated_at")
  private Instant escalatedAt;

  @Column(name = "escalation_source", length = 10)
  private String escalationSource;

  @Column(name = "governance_audit_event_id", length = 36)
  private String governanceAuditEventId;

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

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public Employee getOwner() {
    return owner;
  }

  public void setOwner(Employee owner) {
    this.owner = owner;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getCaseType() {
    return caseType;
  }

  public void setCaseType(String caseType) {
    this.caseType = caseType;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(Instant registeredAt) {
    this.registeredAt = registeredAt;
  }

  public LocalDate getStatutoryDeadline() {
    return statutoryDeadline;
  }

  public void setStatutoryDeadline(LocalDate statutoryDeadline) {
    this.statutoryDeadline = statutoryDeadline;
  }

  public Instant getSlaDueAt() {
    return slaDueAt;
  }

  public void setSlaDueAt(Instant slaDueAt) {
    this.slaDueAt = slaDueAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getEscalatedAt() {
    return escalatedAt;
  }

  public void setEscalatedAt(Instant escalatedAt) {
    this.escalatedAt = escalatedAt;
  }

  public String getEscalationSource() {
    return escalationSource;
  }

  public void setEscalationSource(String escalationSource) {
    this.escalationSource = escalationSource;
  }

  public String getGovernanceAuditEventId() {
    return governanceAuditEventId;
  }

  public void setGovernanceAuditEventId(String governanceAuditEventId) {
    this.governanceAuditEventId = governanceAuditEventId;
  }
}
