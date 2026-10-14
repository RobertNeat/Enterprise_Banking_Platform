package robert_neat.banking_backend.collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.common.EmployeeNames;
import robert_neat.banking_backend.common.NotFoundException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CollectionsService {

    private final CollectionsCaseRepository collectionsCases;
    private final CollectionsActionRepository collectionsActions;

    public CollectionsService(
            CollectionsCaseRepository collectionsCases, CollectionsActionRepository collectionsActions) {
        this.collectionsCases = collectionsCases;
        this.collectionsActions = collectionsActions;
    }

    public List<CollectionsCaseDto> findAll() {
        return collectionsCases.findAll().stream()
                .map(c -> new CollectionsCaseDto(
                        c.getId(),
                        c.getLoanApplication().getCustomer().getFullName(),
                        c.getLoanApplication().getId(),
                        c.getDaysPastDue(),
                        c.getOutstandingAmount(),
                        c.getStage()))
                .toList();
    }

    public List<CollectionsActionDto> findActionsByCaseId(String caseId) {
        if (!collectionsCases.existsById(caseId)) {
            throw new NotFoundException("Collections case not found: " + caseId);
        }
        return collectionsActions.findAll().stream()
                .filter(a -> a.getCollectionsCase().getId().equals(caseId))
                .map(a -> new CollectionsActionDto(
                        a.getActionType(),
                        a.getPerformedAt(),
                        EmployeeNames.abbreviated(a.getPerformedBy()),
                        a.getGovernanceAuditEventId()))
                .toList();
    }
}
