package robert_neat.external_institutions_gateway_mock.enforcement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnforcementService {

    private final EnforcementCaseRepository enforcementCaseRepository;

    public EnforcementService(EnforcementCaseRepository enforcementCaseRepository) {
        this.enforcementCaseRepository = enforcementCaseRepository;
    }

    @Transactional
    public EnforcementCase registerIncoming(IncomingEnforcementRequest request) {
        if (isBlank(request.bailiff()) || isBlank(request.caseNumber()) || isBlank(request.customer())
                || request.blockedAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bailiff, caseNumber, customer i blockedAmount sa wymagane");
        }
        String id = UUID.randomUUID().toString();
        EnforcementCase created = new EnforcementCase(
                id, request.customer(), request.bailiff(), request.caseNumber(),
                request.blockedAmount(), null, request.protectedAmount(),
                EnforcementStatus.REGISTERED, Instant.now(), null);
        return enforcementCaseRepository.save(created);
    }

    @Transactional(readOnly = true)
    public List<EnforcementCase> list() {
        return enforcementCaseRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public SettlementConfirmation transfer(String id, BigDecimal transferredAmount) {
        if (transferredAmount == null || transferredAmount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transferredAmount musi byc > 0");
        }

        EnforcementCase existing = enforcementCaseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zajecie nie znalezione: " + id));
        if (existing.status() == EnforcementStatus.SETTLED || existing.status() == EnforcementStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Zajecie juz rozliczone: " + existing.status());
        }
        EnforcementCase settled = existing.withSettlement(transferredAmount, Instant.now());
        EnforcementCase result = enforcementCaseRepository.save(settled);
        return new SettlementConfirmation(result.id(), result.status(), result.settledAt());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
