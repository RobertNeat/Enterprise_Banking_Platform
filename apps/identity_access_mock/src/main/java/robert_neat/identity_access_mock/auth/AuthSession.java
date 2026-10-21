package robert_neat.identity_access_mock.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity replacing the old {@code sessionsByToken: Map<String, String>} token -> employeeId
 * association. {@code token} is the primary key (lookups/deletes are always by token, never by
 * employeeId), {@code employeeId} a plain FK-like column (REFERENCES enforced in the schema
 * changeset, not a JPA @ManyToOne — open-in-view is false and nothing needs to lazy-load the
 * account from a session).
 */
@Entity
@Table(name = "auth_session", schema = "identity_access")
public class AuthSession {

    @Id
    @Column(name = "token", length = 36)
    private String token;

    @Column(name = "employee_id", length = 20, nullable = false)
    private String employeeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthSession() {
        // JPA
    }

    public AuthSession(String token, String employeeId, Instant createdAt) {
        this.token = token;
        this.employeeId = employeeId;
        this.createdAt = createdAt;
    }

    public String getToken() {
        return token;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
