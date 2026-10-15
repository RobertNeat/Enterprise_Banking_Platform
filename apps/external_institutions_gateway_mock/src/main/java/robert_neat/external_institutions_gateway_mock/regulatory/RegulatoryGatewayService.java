package robert_neat.external_institutions_gateway_mock.regulatory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Pseudo-losowa, ale deterministyczna decyzja (hash reportId mod 10 == 0 -> REJECTED, ~90% ACCEPTED),
 * zeby testy i wywolujacy mogli celowo wywolac znany wynik.
 */
@Service
public class RegulatoryGatewayService {

    private static final Set<String> VALID_RECIPIENTS = Set.of("KNF", "NBP", "BFG", "FATCA/CRS");

    // Fixed gateway-side submission ids matching apps/banking_backend's report_submission seed
    // (db/changelog/seed/010-reporting.sql) so "Sprawdź status" resolves against seeded reports,
    // not just newly submitted ones.
    private static final String SEED_SUBMISSION_NBP_014 = "7f3c2a91-4b6e-4d2a-9c1f-2e8b5d6a0f14";
    private static final String SEED_SUBMISSION_FATCA_009 = "c19e8d42-0a7b-4f3e-8b65-91d4e2f7a308";

    private final Map<String, RegulatorySubmission> submissions = new ConcurrentHashMap<>();
    private final long delayMs;
    private final boolean seedMockData;

    public RegulatoryGatewayService(
            @Value("${gateway.submission.delay-ms:2500}") long delayMs,
            @Value("${gateway.seed-mock-data:true}") boolean seedMockData) {
        this.delayMs = delayMs;
        this.seedMockData = seedMockData;
    }

    @PostConstruct
    void seed() {
        if (!seedMockData) {
            return;
        }
        // RPT-NBP-014: still PENDING in the backend seed (submitted, awaiting gateway resolution).
        // Scheduled to resolve the same way submit() does — otherwise "Sprawdź status" would poll
        // a submission stuck PENDING forever. Deterministic rule (reportId.hashCode() mod 10 == 0
        // -> REJECTED) resolves "RPT-NBP-014" to ACCEPTED.
        submissions.put(SEED_SUBMISSION_NBP_014, new RegulatorySubmission(
                SEED_SUBMISSION_NBP_014, "RPT-NBP-014", "NBP", "Sprawozdanie płynnościowe LCR/NSFR",
                "Wrzesień 2026", SubmissionStatus.PENDING, List.of(),
                Instant.parse("2026-10-03T09:00:00Z"), null));
        CompletableFuture.runAsync(() -> resolve(SEED_SUBMISSION_NBP_014, "RPT-NBP-014"),
                CompletableFuture.delayedExecutor(delayMs, MILLISECONDS));
        // RPT-FATCA-009: already ACCEPTED in the backend seed.
        submissions.put(SEED_SUBMISSION_FATCA_009, new RegulatorySubmission(
                SEED_SUBMISSION_FATCA_009, "RPT-FATCA-009", "FATCA/CRS", "Raport rachunków raportowanych",
                "2025", SubmissionStatus.ACCEPTED, List.of(),
                Instant.parse("2026-06-20T08:00:00Z"), Instant.parse("2026-06-21T07:30:00Z")));
    }

    public RegulatorySubmission submit(RegulatorySubmissionRequest request) {
        if (isBlank(request.reportId()) || isBlank(request.recipient()) || isBlank(request.name())
                || isBlank(request.period())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "reportId, recipient, name i period sa wymagane");
        }
        if (!VALID_RECIPIENTS.contains(request.recipient())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "recipient musi byc jednym z: " + VALID_RECIPIENTS);
        }

        String submissionId = UUID.randomUUID().toString();
        RegulatorySubmission submission = new RegulatorySubmission(
                submissionId, request.reportId(), request.recipient(), request.name(), request.period(),
                SubmissionStatus.PENDING, List.of(), Instant.now(), null);
        submissions.put(submissionId, submission);

        CompletableFuture.runAsync(() -> resolve(submissionId, request.reportId()),
                CompletableFuture.delayedExecutor(delayMs, MILLISECONDS));

        return submission;
    }

    public RegulatorySubmission get(String submissionId) {
        RegulatorySubmission submission = submissions.get(submissionId);
        if (submission == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Zgloszenie nie znalezione: " + submissionId);
        }
        return submission;
    }

    public List<RegulatorySubmission> list() {
        return submissions.values().stream()
                .sorted(Comparator.comparing(RegulatorySubmission::submittedAt))
                .toList();
    }

    private void resolve(String submissionId, String reportId) {
        boolean rejected = Math.floorMod(reportId.hashCode(), 10) == 0;
        SubmissionStatus status = rejected ? SubmissionStatus.REJECTED : SubmissionStatus.ACCEPTED;
        List<String> errors = rejected
                ? List.of("Niezgodnosc struktury danych z wymogami odbiorcy")
                : List.of();
        submissions.computeIfPresent(submissionId, (key, existing) ->
                existing.withResolution(status, errors, Instant.now()));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
