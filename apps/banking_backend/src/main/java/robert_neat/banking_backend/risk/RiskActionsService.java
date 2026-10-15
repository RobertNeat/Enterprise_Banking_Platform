package robert_neat.banking_backend.risk;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.approvals.GovernanceClient;
import robert_neat.banking_backend.common.NotFoundException;
import robert_neat.banking_backend.creditrisk.CreditRiskClient;
import robert_neat.banking_backend.creditrisk.EclClassificationResult;

/**
 * F3 mutating flow — reclassifies a collateral position's IFRS 9 stage/provision via
 * credit_risk_mock's POST /ecl/classify (see creditrisk/CreditRiskClient.java). daysPastDue
 * provenance decision: see ReclassifyCollateralRequest.
 */
@Service
@Transactional
public class RiskActionsService {

    private final CollateralEntryRepository collateralEntries;
    private final CreditRiskClient creditRiskClient;
    private final GovernanceClient governanceClient;

    public RiskActionsService(
            CollateralEntryRepository collateralEntries,
            CreditRiskClient creditRiskClient,
            GovernanceClient governanceClient) {
        this.collateralEntries = collateralEntries;
        this.creditRiskClient = creditRiskClient;
        this.governanceClient = governanceClient;
    }

    public CollateralEntryDto reclassify(String id, int daysPastDue, String employeeId) {
        CollateralEntry entry = collateralEntries.findById(id)
                .orElseThrow(() -> new NotFoundException("Collateral entry not found: " + id));

        String fromStage = entry.getStage();
        EclClassificationResult result = creditRiskClient.classifyEcl(
                id, daysPastDue, entry.getRating(), entry.getValuation());

        entry.setStage(result.stage());
        entry.setProvision(result.provision());
        entry.setEclClassifiedAt(result.classifiedAt());
        collateralEntries.save(entry);

        governanceClient.recordAuditEvent(employeeId, "COLLATERAL_RECLASSIFIED", "COLLATERAL_ENTRY", id,
                fromStage + " -> " + result.stage());

        return toDto(entry);
    }

    static CollateralEntryDto toDto(CollateralEntry c) {
        return new CollateralEntryDto(
                c.getId(),
                c.getCustomer().getFullName(),
                c.getType(),
                c.getValuation(),
                c.getLtv(),
                c.getRating(),
                c.getStage(),
                c.getProvision());
    }
}
