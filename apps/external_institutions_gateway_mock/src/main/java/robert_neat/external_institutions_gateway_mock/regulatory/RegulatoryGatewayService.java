package robert_neat.external_institutions_gateway_mock.regulatory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    private final Map<String, RegulatorySubmission> submissions = new ConcurrentHashMap<>();
    private final long delayMs;

    public RegulatoryGatewayService(@Value("${gateway.submission.delay-ms:2500}") long delayMs) {
        this.delayMs = delayMs;
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
