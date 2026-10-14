package robert_neat.banking_backend.ebanking;

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
@Table(name = "ebanking_user")
public class EbankingUser {

    @Id
    @Column(name = "id", length = 10)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "channel", length = 10, nullable = false)
    private String channel;

    @Column(name = "access_status", length = 15, nullable = false)
    private String accessStatus;

    @Column(name = "daily_limit", precision = 19, scale = 2, nullable = false)
    private BigDecimal dailyLimit;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "permissions", length = 500)
    private String permissions;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(String accessStatus) {
        this.accessStatus = accessStatus;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
