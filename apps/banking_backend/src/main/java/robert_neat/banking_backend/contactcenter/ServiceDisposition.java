package robert_neat.banking_backend.contactcenter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import robert_neat.banking_backend.cases.CustomerCase;

import java.time.LocalDate;

@Entity
@Table(name = "service_disposition")
public class ServiceDisposition {

    @Id
    @Column(name = "id", length = 10)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_case_id", nullable = false)
    private CustomerCase customerCase;

    @Column(name = "type_key", length = 50, nullable = false)
    private String typeKey;

    @Column(name = "status", length = 15, nullable = false)
    private String status;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CustomerCase getCustomerCase() {
        return customerCase;
    }

    public void setCustomerCase(CustomerCase customerCase) {
        this.customerCase = customerCase;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDate occurredAt) {
        this.occurredAt = occurredAt;
    }
}
