package robert_neat.identity_access_mock.auth;

import java.util.List;
import java.util.Map;

/** Static role -> permissions/navigation-module map. Intentionally simple (no persistence, no hierarchy). */
public final class RolePermissions {

    public static final String ADMIN = "Administrator systemu";
    public static final String LOANS = "Doradca kredytowy";
    public static final String RISK = "Analityk ryzyka";
    public static final String COLLECTIONS = "Windykator";
    public static final String CONTACT = "Konsultant kanału kontaktu";
    public static final String COMPLIANCE = "Compliance / AML";

    private static final Map<String, List<String>> PERMISSIONS_BY_ROLE = Map.of(
            ADMIN, List.of("dashboard", "loans", "risk", "collections", "contact", "compliance",
                    "ebanking", "cards", "accounts-admin", "audit"),
            LOANS, List.of("dashboard", "loans", "ebanking"),
            RISK, List.of("dashboard", "risk", "loans"),
            COLLECTIONS, List.of("dashboard", "collections", "enforcement"),
            CONTACT, List.of("dashboard", "contact", "ebanking", "cards"),
            COMPLIANCE, List.of("dashboard", "compliance", "audit")
    );

    private RolePermissions() {
    }

    public static List<String> forRole(String role) {
        return PERMISSIONS_BY_ROLE.getOrDefault(role, List.of());
    }
}
