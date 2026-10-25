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

    // "approvals" (maker-checker queue, R1.5) gates the banking_web approvals screen for the two
    // roles with seniorityLevel high enough to ever decide anything (see banking_backend's
    // RoleCode) — a separate concern from the hierarchy itself, which intentionally stays out of
    // this map (see class javadoc). Not a breaking change to existing demo accounts' other values.
    private static final Map<String, List<String>> PERMISSIONS_BY_ROLE = Map.of(
            ADMIN, List.of("dashboard", "loans", "risk", "collections", "contact", "compliance",
                    "ebanking", "cards", "accounts-admin", "audit", "approvals", "enforcement"),
            LOANS, List.of("dashboard", "loans", "ebanking"),
            RISK, List.of("dashboard", "risk", "loans"),
            COLLECTIONS, List.of("dashboard", "collections", "enforcement"),
            CONTACT, List.of("dashboard", "contact", "ebanking", "cards"),
            COMPLIANCE, List.of("dashboard", "compliance", "audit", "approvals")
    );

    private RolePermissions() {
    }

    public static List<String> forRole(String role) {
        if (role == null) {
            return List.of();
        }
        return PERMISSIONS_BY_ROLE.getOrDefault(role, List.of());
    }

    /** Used by the admin approval flow to reject an unmapped role before it reaches RoleCode.fromDisplayName. */
    public static boolean isKnownRole(String role) {
        return role != null && PERMISSIONS_BY_ROLE.containsKey(role);
    }
}
