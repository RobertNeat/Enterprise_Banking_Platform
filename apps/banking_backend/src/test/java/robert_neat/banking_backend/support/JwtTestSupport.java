package robert_neat.banking_backend.support;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Shared JWT post-processor for MockMvc requests — every endpoint except /actuator/health,
 * /auth/login and /auth/register requires authentication (see SecurityConfig). Authorities are
 * irrelevant here: the security chain only checks "authenticated", not specific permissions.
 */
public final class JwtTestSupport {

  private JwtTestSupport() {}

  public static RequestPostProcessor testJwt() {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(
            jwt ->
                jwt.subject("PRC-00001")
                    .claim("role", "Administrator systemu")
                    .claim(
                        "permissions",
                        java.util.List.of(
                            "dashboard",
                            "loans",
                            "risk",
                            "collections",
                            "contact",
                            "compliance",
                            "ebanking",
                            "cards",
                            "accounts-admin",
                            "audit"))
                    .claim("seniorityLevel", 3));
  }

  /**
   * Variant with a caller-chosen subject (employeeId) and seniorityLevel, for maker-checker tests.
   */
  public static RequestPostProcessor testJwt(String subject, int seniorityLevel) {
    return SecurityMockMvcRequestPostProcessors.jwt()
        .jwt(
            jwt ->
                jwt.subject(subject)
                    .claim("role", "ignored-in-these-tests")
                    .claim("permissions", java.util.List.of("dashboard"))
                    .claim("seniorityLevel", seniorityLevel));
  }
}
