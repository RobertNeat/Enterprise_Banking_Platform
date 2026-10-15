package robert_neat.banking_backend.risk;

/**
 * F3 — daysPastDue is operator-entered (decision: CollateralEntry has no days-past-due of its own
 * and no reliable 1:1 link to a CollectionsCase — a customer can have several loans/collections
 * cases, so deriving it automatically would be ambiguous; the employee reclassifying the position
 * knows the current delinquency and enters it directly, see docs/services/banking_backend.md).
 */
public record ReclassifyCollateralRequest(int daysPastDue) {
}
