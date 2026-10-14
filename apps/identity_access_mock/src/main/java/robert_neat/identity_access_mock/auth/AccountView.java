package robert_neat.identity_access_mock.auth;

/** Account projection without the password, for GET /auth/accounts and the operator UI. */
public record AccountView(String id, String employeeId, String role, AccountStatus status,
                           String firstName, String lastName, String email) {

    public static AccountView from(EmployeeAccount account) {
        return new AccountView(account.getId(), account.getEmployeeId(), account.getRole(),
                account.getStatus(), account.getFirstName(), account.getLastName(), account.getEmail());
    }
}
