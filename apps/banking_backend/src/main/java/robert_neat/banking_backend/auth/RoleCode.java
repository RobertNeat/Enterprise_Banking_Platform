package robert_neat.banking_backend.auth;

import java.util.Arrays;

/**
 * Stable role codes matching the demo account `id` values used in banking_web's
 * DEMO_ACCOUNTS list, mapped 1:1 from the Polish role text returned by identity_access_mock
 * (see RolePermissions in that service). Single place for this mapping — permissions
 * themselves are NOT duplicated here, they come from identity_access_mock's response.
 *
 * seniorityLevel is banking_backend's own addition (NOT present in identity_access_mock,
 * which intentionally has no hierarchy — see RolePermissions there) used for maker-checker
 * approval gating: a checker may decide an approval requested by a role with a strictly
 * lower seniorityLevel. ADMIN=3 (decides anyone below), COMPLIANCE=2 (AML/compliance acts as
 * second-level checker over the 4 frontline roles), LOANS/RISK/COLLECTIONS/CONTACT=1 (maker-only,
 * cannot decide each other's approvals). This is flat on purpose for the 4 frontline roles —
 * no business reason found to rank a credit analyst above a loan advisor or vice versa.
 *
 * ADMIN holds the highest seniorityLevel in the system, so no role can ever check an approval
 * ADMIN requested (hasHigherSeniorityRole() is false for it) — see ApprovalsService.requireCheckerExists(),
 * which blocks ADMIN from initiating a maker-checker approval in the first place rather than
 * letting it get stuck PENDING forever.
 */
public enum RoleCode {

    ADMIN("Administrator systemu", 3),
    COMPLIANCE("Compliance / AML", 2),
    LOANS("Doradca kredytowy", 1),
    RISK("Analityk ryzyka", 1),
    COLLECTIONS("Windykator", 1),
    CONTACT("Konsultant kanału kontaktu", 1);

    private final String displayName;
    private final int seniorityLevel;

    RoleCode(String displayName, int seniorityLevel) {
        this.displayName = displayName;
        this.seniorityLevel = seniorityLevel;
    }

    public String displayName() {
        return displayName;
    }

    /** Higher = can decide (maker-checker) approvals requested by lower-seniority roles. */
    public int seniorityLevel() {
        return seniorityLevel;
    }

    /** False means no role in the system could ever check an approval this role requests. */
    public boolean hasHigherSeniorityRole() {
        return Arrays.stream(values()).anyMatch(role -> role.seniorityLevel > this.seniorityLevel);
    }

    /** Stable lowercase code matching the demo account `id` (admin, loans, risk, ...). */
    public String code() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** @throws UnmappedRoleException if the role text is not one of the known demo roles. */
    public static RoleCode fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(code -> code.displayName.equals(displayName))
                .findFirst()
                .orElseThrow(() -> new UnmappedRoleException(displayName));
    }
}
