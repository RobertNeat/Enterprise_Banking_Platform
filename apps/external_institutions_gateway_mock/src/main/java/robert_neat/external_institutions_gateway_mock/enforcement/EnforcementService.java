package robert_neat.external_institutions_gateway_mock.enforcement;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class EnforcementService {

    private final Map<String, EnforcementCase> cases = new ConcurrentHashMap<>();

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
        cases.put(id, created);
        return created;
    }

    public List<EnforcementCase> list() {
        return cases.values().stream()
                .sorted(Comparator.comparing(EnforcementCase::createdAt))
                .toList();
    }

    public SettlementConfirmation transfer(String id, BigDecimal transferredAmount) {
        if (transferredAmount == null || transferredAmount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transferredAmount musi byc > 0");
        }

        AtomicReference<EnforcementCase> updated = new AtomicReference<>();
        cases.compute(id, (key, existing) -> {
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Zajecie nie znalezione: " + id);
            }
            if (existing.status() == EnforcementStatus.SETTLED || existing.status() == EnforcementStatus.CLOSED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Zajecie juz rozliczone: " + existing.status());
            }
            EnforcementCase settled = existing.withSettlement(transferredAmount, Instant.now());
            updated.set(settled);
            return settled;
        });

        EnforcementCase result = updated.get();
        return new SettlementConfirmation(result.id(), result.status(), result.settledAt());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
