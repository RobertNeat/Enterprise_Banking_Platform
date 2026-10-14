package robert_neat.banking_backend.enforcement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnforcementService {

    private final EnforcementCaseRepository enforcementCases;

    public EnforcementService(EnforcementCaseRepository enforcementCases) {
        this.enforcementCases = enforcementCases;
    }

    public List<EnforcementCaseDto> findAll() {
        return enforcementCases.findAll().stream()
                .map(e -> new EnforcementCaseDto(
                        e.getId(),
                        e.getCustomer().getFullName(),
                        e.getBailiff(),
                        e.getCaseNumber(),
                        e.getBlockedAmount(),
                        e.getTransferredAmount() == null ? BigDecimal.ZERO : e.getTransferredAmount(),
                        e.getProtectedAmount(),
                        e.getStatus()))
                .toList();
    }
}
