package robert_neat.banking_backend.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import robert_neat.banking_backend.customer.Customer;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @Column(name = "id", length = 10)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "segment", length = 10, nullable = false)
    private String segment;

    @Column(name = "product", length = 20, nullable = false)
    private String product;

    @Column(name = "stage", length = 20, nullable = false)
    private String stage;

    @Column(name = "requested_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "score_result")
    private Integer scoreResult;

    @Column(name = "dsti", precision = 4, scale = 3)
    private BigDecimal dsti;

    @Column(name = "bureau_inquiry_id", length = 36)
    private String bureauInquiryId;

    @Column(name = "interest_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public Integer getScoreResult() {
        return scoreResult;
    }

    public void setScoreResult(Integer scoreResult) {
        this.scoreResult = scoreResult;
    }

    public BigDecimal getDsti() {
        return dsti;
    }

    public void setDsti(BigDecimal dsti) {
        this.dsti = dsti;
    }

    public String getBureauInquiryId() {
        return bureauInquiryId;
    }

    public void setBureauInquiryId(String bureauInquiryId) {
        this.bureauInquiryId = bureauInquiryId;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
