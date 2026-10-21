package robert_neat.external_institutions_gateway_mock.regulatory;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Pseudo-losowa, ale deterministyczna decyzja (hash reportId mod 10 == 0 -> REJECTED, ~90% ACCEPTED),
 * zeby testy i wywolujacy mogli celowo wywolac znany wynik.
 *
 * <p>resolve() runs on a delayed-executor thread, not the request thread, so it cannot use
 * {@code @Transactional} (a self-invocation bypasses the Spring AOP proxy entirely) -- it uses an
 * injected {@link TransactionTemplate} instead. The resolve task for a submission is always
 * scheduled only after that submission's insert/seed has committed (see submit()'s
 * afterCommit synchronization and the ApplicationReadyEvent reschedule below for PENDING rows left
 * over from Liquibase seeding) -- otherwise resolve()'s row lock could run before the insert is
 * visible to other transactions and silently find nothing to update.
 */
@Service
public class RegulatoryGatewayService {

    private static final Set<String> VALID_RECIPIENTS = Set.of("KNF", "NBP", "BFG", "FATCA/CRS");

    private final RegulatorySubmissionRepository regulatorySubmissionRepository;
    private final TransactionTemplate transactionTemplate;
    private final long delayMs;

    public RegulatoryGatewayService(
            RegulatorySubmissionRepository regulatorySubmissionRepository,
            TransactionTemplate transactionTemplate,
            @Value("${gateway.submission.delay-ms:2500}") long delayMs) {
        this.regulatorySubmissionRepository = regulatorySubmissionRepository;
        this.transactionTemplate = transactionTemplate;
        this.delayMs = delayMs;
    }

    /**
     * Reschedules resolution for any submission left PENDING by a prior run (e.g. the Liquibase
     * seed's RPT-NBP-014, or a submission that was mid-flight when the container stopped) -- the
     * old @PostConstruct-scheduled seed resolve has no equivalent once seeding moves to Liquibase,
     * so without this the seeded PENDING row would never resolve. resolve() is a no-op once a
     * submission is no longer PENDING, so scheduling it twice (e.g. once here, once from a fresh
     * submit()) is harmless.
     */
    @EventListener(ApplicationReadyEvent.class)
    void reschedulePendingSubmissions() {
        for (RegulatorySubmission pending : regulatorySubmissionRepository.findByStatus(SubmissionStatus.PENDING)) {
            scheduleResolve(pending.submissionId(), pending.reportId());
        }
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
        RegulatorySubmission saved = transactionTemplate.execute(status ->
                regulatorySubmissionRepository.save(submission));

        // Scheduled only after the insert transaction above has committed, so the delayed resolve()
        // call is guaranteed to see the row.
        scheduleResolve(submissionId, request.reportId());

        return saved;
    }

    public RegulatorySubmission get(String submissionId) {
        return regulatorySubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zgloszenie nie znalezione: " + submissionId));
    }

    public List<RegulatorySubmission> list() {
        return regulatorySubmissionRepository.findAllByOrderBySubmittedAtAsc();
    }

    private void scheduleResolve(String submissionId, String reportId) {
        Runnable schedule = () -> CompletableFuture.runAsync(() -> resolve(submissionId, reportId),
                CompletableFuture.delayedExecutor(delayMs, MILLISECONDS));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule.run();
                }
            });
        } else {
            schedule.run();
        }
    }

    /** Locks the row and resolves it; a no-op if another resolve already ran first (status != PENDING). */
    private void resolve(String submissionId, String reportId) {
        transactionTemplate.executeWithoutResult(status -> {
            RegulatorySubmission existing = regulatorySubmissionRepository.findByIdForUpdate(submissionId)
                    .orElse(null);
            if (existing == null || existing.status() != SubmissionStatus.PENDING) {
                return;
            }
            boolean rejected = Math.floorMod(reportId.hashCode(), 10) == 0;
            SubmissionStatus newStatus = rejected ? SubmissionStatus.REJECTED : SubmissionStatus.ACCEPTED;
            List<String> errors = rejected
                    ? List.of("Niezgodnosc struktury danych z wymogami odbiorcy")
                    : List.of();
            regulatorySubmissionRepository.save(existing.withResolution(newStatus, errors, Instant.now()));
        });
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
