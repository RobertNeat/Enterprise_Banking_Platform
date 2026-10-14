package robert_neat.identity_access_mock.auth;

import org.springframework.stereotype.Service;
import robert_neat.identity_access_mock.common.ConflictException;
import robert_neat.identity_access_mock.common.ForbiddenException;
import robert_neat.identity_access_mock.common.UnauthorizedException;
import robert_neat.identity_access_mock.common.ValidationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** In-memory RBAC: accounts + active session tokens. Session storage is a simple token -> employeeId map. */
@Service
public class AuthService {

    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("^PRC-\\d{5}$");

    private final Map<String, EmployeeAccount> accountsByEmployeeId = new ConcurrentHashMap<>();
    private final Map<String, String> sessionsByToken = new ConcurrentHashMap<>();

    public AuthService() {
        seedDemoAccounts();
    }

    private void seedDemoAccounts() {
        register("admin", "PRC-00001", "Admin12345", RolePermissions.ADMIN);
        register("loans", "PRC-10234", "Loans12345", RolePermissions.LOANS);
        register("risk", "PRC-20456", "Risk123456", RolePermissions.RISK);
        register("collections", "PRC-30678", "Collect1234", RolePermissions.COLLECTIONS);
        register("contact", "PRC-40890", "Contact1234", RolePermissions.CONTACT);
        register("compliance", "PRC-50123", "Compliance1", RolePermissions.COMPLIANCE);
    }

    private void register(String id, String employeeId, String password, String role) {
        accountsByEmployeeId.put(employeeId, new EmployeeAccount(id, employeeId, password, role,
                AccountStatus.ACTIVE, RolePermissions.forRole(role)));
    }

    public LoginResponse login(LoginRequest request) {
        if (request.employeeId() == null || request.password() == null) {
            throw new ValidationException("employeeId and password are required");
        }
        EmployeeAccount account = accountsByEmployeeId.get(request.employeeId());
        if (account == null || !account.getPassword().equals(request.password())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active: " + account.getStatus());
        }
        String token = UUID.randomUUID().toString();
        sessionsByToken.put(token, account.getEmployeeId());
        return new LoginResponse(token, account.getEmployeeId(), account.getRole(), account.getPermissions());
    }

    public void logout(String token) {
        if (token != null) {
            sessionsByToken.remove(token);
        }
    }

    public EmployeeAccount register(RegisterRequest request) {
        if (request.employment() == null || request.employment().employeeId() == null
                || !EMPLOYEE_ID_PATTERN.matcher(request.employment().employeeId()).matches()) {
            throw new ValidationException("employeeId must match PRC-00000 format");
        }
        if (request.account() == null || request.account().password() == null
                || request.account().password().length() < 8) {
            throw new ValidationException("password must be at least 8 characters");
        }
        if (!request.account().password().equals(request.account().confirmPassword())) {
            throw new ValidationException("password and confirmPassword must match");
        }
        String employeeId = request.employment().employeeId();
        if (accountsByEmployeeId.containsKey(employeeId)) {
            throw new ConflictException("employeeId already registered: " + employeeId);
        }
        EmployeeAccount account = new EmployeeAccount(UUID.randomUUID().toString(), employeeId,
                request.account().password(), null, AccountStatus.PENDING_APPROVAL, List.of());
        account.setFirstName(request.personal() != null ? request.personal().firstName() : null);
        account.setLastName(request.personal() != null ? request.personal().lastName() : null);
        account.setEmail(request.personal() != null ? request.personal().email() : null);
        accountsByEmployeeId.put(employeeId, account);
        return account;
    }

    public List<AccountView> listAccounts() {
        return accountsByEmployeeId.values().stream().map(AccountView::from).toList();
    }

    /** Used by OTP/device/e-banking flows to confirm the employeeId exists. */
    public boolean accountExists(String employeeId) {
        return accountsByEmployeeId.containsKey(employeeId);
    }
}
