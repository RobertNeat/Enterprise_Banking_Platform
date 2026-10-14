package robert_neat.banking_backend.enforcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import robert_neat.banking_backend.customer.BankAccount;
import robert_neat.banking_backend.customer.Customer;

import java.math.BigDecimal;

@Entity
@Table(name = "enforcement_case")
public class EnforcementCase {

    @Id
    @Column(name = "id", length = 10)
    private String id;

    @Column(name = "gateway_enforcement_id", length = 36)
    private String gatewayEnforcementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(name = "bailiff", length = 200, nullable = false)
    private String bailiff;

    @Column(name = "case_number", length = 50, nullable = false)
    private String caseNumber;

    @Column(name = "kind", length = 15, nullable = false)
    private String kind;

    @Column(name = "blocked_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal blockedAmount;

    @Column(name = "transferred_amount", precision = 19, scale = 2)
    private BigDecimal transferredAmount;

    @Column(name = "protected_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal protectedAmount;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @Column(name = "approval_id", length = 36)
    private String approvalId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGatewayEnforcementId() {
        return gatewayEnforcementId;
    }

    public void setGatewayEnforcementId(String gatewayEnforcementId) {
        this.gatewayEnforcementId = gatewayEnforcementId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBailiff() {
        return bailiff;
    }

    public void setBailiff(String bailiff) {
        this.bailiff = bailiff;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public BigDecimal getBlockedAmount() {
        return blockedAmount;
    }

    public void setBlockedAmount(BigDecimal blockedAmount) {
        this.blockedAmount = blockedAmount;
    }

    public BigDecimal getTransferredAmount() {
        return transferredAmount;
    }

    public void setTransferredAmount(BigDecimal transferredAmount) {
        this.transferredAmount = transferredAmount;
    }

    public BigDecimal getProtectedAmount() {
        return protectedAmount;
    }

    public void setProtectedAmount(BigDecimal protectedAmount) {
        this.protectedAmount = protectedAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }
}
