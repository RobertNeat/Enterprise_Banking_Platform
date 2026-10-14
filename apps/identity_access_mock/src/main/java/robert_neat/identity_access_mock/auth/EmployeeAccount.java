package robert_neat.identity_access_mock.auth;

import java.util.List;

/** In-memory account record. Mirrors DemoAccount (login.page.ts) plus registration/status fields. */
public class EmployeeAccount {

    private final String id;
    private final String employeeId;
    private String password;
    private String role;
    private AccountStatus status;
    private List<String> permissions;
    private String firstName;
    private String lastName;
    private String email;

    public EmployeeAccount(String id, String employeeId, String password, String role,
                            AccountStatus status, List<String> permissions) {
        this.id = id;
        this.employeeId = employeeId;
        this.password = password;
        this.role = role;
        this.status = status;
        this.permissions = permissions;
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

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
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
