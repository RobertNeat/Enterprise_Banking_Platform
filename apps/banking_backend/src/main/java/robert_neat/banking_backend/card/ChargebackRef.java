package robert_neat.banking_backend.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chargeback_ref")
public class ChargebackRef {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_authorization_ref_id", nullable = false)
    private CardAuthorizationRef cardAuthorizationRef;

    @Column(name = "reason_code", length = 4, nullable = false)
    private String reasonCode;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "filed_at", nullable = false)
    private Instant filedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CardAuthorizationRef getCardAuthorizationRef() {
        return cardAuthorizationRef;
    }

    public void setCardAuthorizationRef(CardAuthorizationRef cardAuthorizationRef) {
        this.cardAuthorizationRef = cardAuthorizationRef;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getFiledAt() {
        return filedAt;
    }

    public void setFiledAt(Instant filedAt) {
        this.filedAt = filedAt;
    }
}
