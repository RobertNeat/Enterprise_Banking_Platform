package robert_neat.identity_access_mock.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.identity_access_mock.common.ConflictException;
import robert_neat.identity_access_mock.common.ForbiddenException;
import robert_neat.identity_access_mock.common.UnauthorizedException;
import robert_neat.identity_access_mock.common.ValidationException;

/** RBAC: accounts + active session tokens, persisted via JPA. */
@Service
public class AuthService {

    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("^PRC-\\d{5}$");

    private final EmployeeAccountRepository accountRepository;
    private final AuthSessionRepository sessionRepository;

    public AuthService(EmployeeAccountRepository accountRepository, AuthSessionRepository sessionRepository) {
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.employeeId() == null || request.password() == null) {
            throw new ValidationException("employeeId and password are required");
        }
        EmployeeAccount account = accountRepository.findByEmployeeId(request.employeeId())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!account.getPassword().equals(request.password())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active: " + account.getStatus());
        }
        String token = UUID.randomUUID().toString();
        sessionRepository.save(new AuthSession(token, account.getEmployeeId(), Instant.now()));
        return new LoginResponse(token, account.getEmployeeId(), account.getRole(), account.getPermissions());
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            sessionRepository.deleteById(token);
        }
    }

    @Transactional
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
        if (accountRepository.existsByEmployeeId(employeeId)) {
            throw new ConflictException("employeeId already registered: " + employeeId);
        }
        EmployeeAccount account = new EmployeeAccount(UUID.randomUUID().toString(), employeeId,
                request.account().password(), null, AccountStatus.PENDING_APPROVAL, List.of());
        account.setFirstName(request.personal() != null ? request.personal().firstName() : null);
        account.setLastName(request.personal() != null ? request.personal().lastName() : null);
        account.setEmail(request.personal() != null ? request.personal().email() : null);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<AccountView> listAccounts() {
        return accountRepository.findAllByOrderByEmployeeIdAsc().stream().map(AccountView::from).toList();
    }

    /** Used by OTP/device/e-banking flows to confirm the employeeId exists. */
    @Transactional(readOnly = true)
    public boolean accountExists(String employeeId) {
        return accountRepository.existsByEmployeeId(employeeId);
    }
}
