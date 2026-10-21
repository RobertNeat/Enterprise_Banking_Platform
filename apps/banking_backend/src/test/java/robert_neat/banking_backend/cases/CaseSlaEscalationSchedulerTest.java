package robert_neat.banking_backend.cases;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pure Mockito test — exercises CaseSlaEscalationScheduler.escalateOverdueCases() directly instead
 * of waiting for the real @Scheduled interval. No Spring context: faster, and avoids the
 * banking.cases.sla-escalation.enabled toggle entirely (that toggle only matters for bean wiring,
 * covered implicitly by the test classpath's application.properties disabling it).
 */
@ExtendWith(MockitoExtension.class)
class CaseSlaEscalationSchedulerTest {

    @Mock
    private CustomerCaseRepository customerCases;

    @Mock
    private CaseActionsService caseActionsService;

    @Captor
    private ArgumentCaptor<Instant> instantCaptor;

    @Captor
    private ArgumentCaptor<Collection<String>> statusesCaptor;

    // Built per-test (not a field initializer) since @Mock fields are only populated by
    // MockitoExtension after field injection, which runs before @Test but after construction.
    private CaseSlaEscalationScheduler scheduler() {
        return new CaseSlaEscalationScheduler(customerCases, caseActionsService);
    }

    @Test
    void queriesOverdueOpenCasesAndEscalatesEachOne() {
        CustomerCase first = caseWithId("CS-10001");
        CustomerCase second = caseWithId("CS-10002");
        given(customerCases.findBySlaDueAtBeforeAndStatusIn(any(), any())).willReturn(List.of(first, second));

        scheduler().escalateOverdueCases();

        verify(customerCases).findBySlaDueAtBeforeAndStatusIn(instantCaptor.capture(), statusesCaptor.capture());
        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder("new", "inProgress");
        verify(caseActionsService).autoEscalateIfSlaExpired("CS-10001");
        verify(caseActionsService).autoEscalateIfSlaExpired("CS-10002");
    }

    @Test
    void continuesSweepWhenOneCaseFails() {
        CustomerCase first = caseWithId("CS-20001");
        CustomerCase second = caseWithId("CS-20002");
        given(customerCases.findBySlaDueAtBeforeAndStatusIn(any(), any())).willReturn(List.of(first, second));
        doThrow(new RuntimeException("governance_mock unreachable"))
                .when(caseActionsService).autoEscalateIfSlaExpired("CS-20001");

        scheduler().escalateOverdueCases();

        verify(caseActionsService).autoEscalateIfSlaExpired("CS-20001");
        verify(caseActionsService).autoEscalateIfSlaExpired("CS-20002");
    }

    @Test
    void doesNothingWhenNoCasesAreOverdue() {
        given(customerCases.findBySlaDueAtBeforeAndStatusIn(any(), any())).willReturn(List.of());

        scheduler().escalateOverdueCases();

        verify(caseActionsService, never()).autoEscalateIfSlaExpired(any());
    }

    private static CustomerCase caseWithId(String id) {
        CustomerCase customerCase = new CustomerCase();
        customerCase.setId(id);
        return customerCase;
    }
}
