package robert_neat.identity_access_mock.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.List;

/**
 * JPA entity for an employee account. Mirrors DemoAccount (login.page.ts) plus registration/status
 * fields. Permissions are derived from {@code role} via {@link RolePermissions} rather than
 * persisted (same "no persistence, no hierarchy" contract as before the migration) — kept as a
 * transient getter so JSON/LoginResponse shape is unchanged.
 */
@Entity
@Table(name = "employee_account", schema = "identity_access")
public class EmployeeAccount {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "employee_id", length = 20, nullable = false, unique = true)
    private String employeeId;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "role", length = 50)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AccountStatus status;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 150)
    private String email;

    protected EmployeeAccount() {
        // JPA
    }

    public EmployeeAccount(String id, String employeeId, String password, String role,
            AccountStatus status, List<String> permissions) {
        this.id = id;
        this.employeeId = employeeId;
        this.password = password;
        this.role = role;
        this.status = status;
        // permissions param kept for call-site compatibility; derived from role instead (see class javadoc).
    }

    public String getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Transient
    public List<String> getPermissions() {
        return RolePermissions.forRole(role);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
