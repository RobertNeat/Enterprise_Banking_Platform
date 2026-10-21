package robert_neat.external_institutions_gateway_mock.enforcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for an e-bailiff enforcement case. {@code id} doubles as the gateway-side
 * "gatewayEnforcementId" referenced by banking_backend's enforcement_case seed — there is no
 * separate id field. Keeps record-style accessors (id(), customer(), ...) alongside JavaBean
 * getters so existing call sites and tests keep compiling unchanged; Jackson serializes via the
 * JavaBean getters only (same dual-accessor pattern as governance_mock's ApprovalRequestRecord).
 */
@Entity
@Table(name = "enforcement_case", schema = "gateway")
public class EnforcementCase {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "customer", length = 50, nullable = false)
    private String customer;

    @Column(name = "bailiff", length = 150, nullable = false)
    private String bailiff;

    @Column(name = "case_number", length = 50, nullable = false)
    private String caseNumber;

    @Column(name = "blocked_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal blockedAmount;

    @Column(name = "transferred_amount", precision = 19, scale = 2)
    private BigDecimal transferredAmount;

    @Column(name = "protected_amount", precision = 19, scale = 2)
    private BigDecimal protectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private EnforcementStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    protected EnforcementCase() {
        // JPA
    }

    public EnforcementCase(
            String id,
            String customer,
            String bailiff,
            String caseNumber,
            BigDecimal blockedAmount,
            BigDecimal transferredAmount,
            BigDecimal protectedAmount,
            EnforcementStatus status,
            Instant createdAt,
            Instant settledAt) {
        this.id = id;
        this.customer = customer;
        this.bailiff = bailiff;
        this.caseNumber = caseNumber;
        this.blockedAmount = blockedAmount;
        this.transferredAmount = transferredAmount;
        this.protectedAmount = protectedAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    public EnforcementCase withSettlement(BigDecimal transferredAmount, Instant settledAt) {
        return new EnforcementCase(id, customer, bailiff, caseNumber, blockedAmount, transferredAmount,
                protectedAmount, EnforcementStatus.SETTLED, createdAt, settledAt);
    }

    // Record-style accessors, kept for existing call sites (tests, UI templates).
    public String id() {
        return id;
    }

    public String customer() {
        return customer;
    }

    public String bailiff() {
        return bailiff;
    }

    public String caseNumber() {
        return caseNumber;
    }

    public BigDecimal blockedAmount() {
        return blockedAmount;
    }

    public BigDecimal transferredAmount() {
        return transferredAmount;
    }

    public BigDecimal protectedAmount() {
        return protectedAmount;
    }

    public EnforcementStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant settledAt() {
        return settledAt;
    }

    // JavaBean getters, used by Jackson for JSON serialization.
    public String getId() {
        return id;
    }

    public String getCustomer() {
        return customer;
    }

    public String getBailiff() {
        return bailiff;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public BigDecimal getBlockedAmount() {
        return blockedAmount;
    }

    public BigDecimal getTransferredAmount() {
        return transferredAmount;
    }

    public BigDecimal getProtectedAmount() {
        return protectedAmount;
    }

    public EnforcementStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }
}
