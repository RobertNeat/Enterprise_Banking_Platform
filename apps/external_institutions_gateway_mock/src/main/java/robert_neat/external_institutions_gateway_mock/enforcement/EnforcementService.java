package robert_neat.external_institutions_gateway_mock.enforcement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
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

    // Fixed gateway-side ids matching apps/banking_backend's enforcement_case seed
    // (db/changelog/seed/009-enforcement.sql) by case/customer so demo lookups line up; the
    // backend's own enforcement_case.gateway_enforcement_id column is NULL in that seed (no link
    // wired up yet — out of scope for R1.5, see docs/services/external_institutions_gateway_mock.md).
    private static final String SEED_EN_2201 = "33333333-3333-4333-8333-333333333001";
    private static final String SEED_EN_2198 = "33333333-3333-4333-8333-333333333002";
    private static final String SEED_EN_2190 = "33333333-3333-4333-8333-333333333003";
    private static final String SEED_EN_2205 = "33333333-3333-4333-8333-333333333004";

    private final Map<String, EnforcementCase> cases = new ConcurrentHashMap<>();
    private final boolean seedMockData;

    public EnforcementService(@Value("${gateway.seed-mock-data:true}") boolean seedMockData) {
        this.seedMockData = seedMockData;
    }

    @PostConstruct
    void seed() {
        if (!seedMockData) {
            return;
        }
        // EN-2201: active, partially settled (blocked 18400.00, transferred so far 6200.00).
        put(SEED_EN_2201, "CUST-006", "Komornik Sądowy A. Dąbrowski", "KM 1123/26",
                new BigDecimal("18400.00"), null, new BigDecimal("1631.40"),
                EnforcementStatus.ACTIVE, Instant.parse("2026-07-10T09:00:00Z"), null);
        // EN-2198: fully settled (blocked == transferred).
        put(SEED_EN_2198, "CUST-004", "Komornik Sądowy M. Wójcik", "KM 998/26",
                new BigDecimal("4200.00"), new BigDecimal("4200.00"), new BigDecimal("1631.40"),
                EnforcementStatus.SETTLED, Instant.parse("2026-05-02T09:00:00Z"),
                Instant.parse("2026-05-20T12:00:00Z"));
        // EN-2190: closed (fully transferred, case formally closed afterwards).
        put(SEED_EN_2190, "CUST-005", "Komornik Sądowy T. Nowicki", "KM 887/25",
                new BigDecimal("128000.00"), new BigDecimal("128000.00"), BigDecimal.ZERO,
                EnforcementStatus.CLOSED, Instant.parse("2025-11-15T09:00:00Z"),
                Instant.parse("2025-12-01T09:00:00Z"));
        // EN-2205: just registered, nothing blocked/transferred yet.
        put(SEED_EN_2205, "CUST-002", "Komornik Sądowy A. Dąbrowski", "KM 1145/26",
                BigDecimal.ZERO, null, new BigDecimal("1631.40"),
                EnforcementStatus.REGISTERED, Instant.parse("2026-10-01T09:00:00Z"), null);
    }

    private void put(String id, String customer, String bailiff, String caseNumber,
            BigDecimal blockedAmount, BigDecimal transferredAmount, BigDecimal protectedAmount,
            EnforcementStatus status, Instant createdAt, Instant settledAt) {
        cases.put(id, new EnforcementCase(id, customer, bailiff, caseNumber, blockedAmount,
                transferredAmount, protectedAmount, status, createdAt, settledAt));
    }

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
