package com.demo.railbridge.bridge;

import com.demo.railbridge.sdk.SdkErrorCode;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BridgeRequestCoordinatorTest {

    @Test
    public void begin_makesRequestVisibleInSnapshot() {
        FakeClock clock = new FakeClock(1_000L);
        BridgeRequestCoordinator coordinator = new BridgeRequestCoordinator(5_000L, clock);

        coordinator.begin("corr-1", "requestCharge", "cb-1", "callback_loss", "2026-04-08T10:00:00Z");
        clock.advance(850L);
        List<InFlightRequestRecord> snapshot = coordinator.snapshot();

        assertEquals(1, snapshot.size());
        assertEquals("corr-1", snapshot.get(0).getCorrelationId());
        assertEquals("requestCharge", snapshot.get(0).getMethod());
        assertEquals("cb-1", snapshot.get(0).getCallbackId());
        assertEquals("pending", snapshot.get(0).getState());
        assertEquals("callback_loss", snapshot.get(0).getScenario());
        assertEquals(850L, snapshot.get(0).getElapsedMs());
    }

    @Test
    public void acceptSuccess_suppressesDuplicateCallbacksAfterFirstTerminalOutcome() {
        BridgeRequestCoordinator coordinator = new BridgeRequestCoordinator(5_000L, new FakeClock(2_000L));
        coordinator.begin("corr-2", "requestCharge", "cb-2", "duplicate_callback", "2026-04-08T10:00:01Z");

        BridgeRequestCoordinator.Decision first = coordinator.acceptSuccess("corr-2", 0);
        BridgeRequestCoordinator.Decision second = coordinator.acceptSuccess("corr-2", 0);

        assertTrue(first.isAccepted());
        assertEquals(BridgeRequestCoordinator.TerminalState.SUCCESS, first.getTerminalState());
        assertFalse(second.isAccepted());
        assertEquals(BridgeRequestCoordinator.TerminalState.SUCCESS, second.getTerminalState());
        assertTrue(second.isTerminalAlreadyReached());
    }

    @Test
    public void acceptTimeout_onlyFiresOnceAndOnlyAfterDeadline() {
        FakeClock clock = new FakeClock(10_000L);
        BridgeRequestCoordinator coordinator = new BridgeRequestCoordinator(5_000L, clock);
        coordinator.begin("corr-3", "getBalance", null, "callback_loss", "2026-04-08T10:00:02Z");

        BridgeRequestCoordinator.Decision early = coordinator.acceptTimeout("corr-3");
        clock.advance(5_000L);
        BridgeRequestCoordinator.Decision first = coordinator.acceptTimeout("corr-3");
        BridgeRequestCoordinator.Decision second = coordinator.acceptTimeout("corr-3");

        assertFalse(early.isAccepted());
        assertEquals("not_due", early.getReason());
        assertEquals(BridgeRequestCoordinator.TerminalState.PENDING, early.getTerminalState());
        assertTrue(first.isAccepted());
        assertEquals(BridgeRequestCoordinator.TerminalState.TIMED_OUT, first.getTerminalState());
        assertFalse(second.isAccepted());
        assertEquals(BridgeRequestCoordinator.TerminalState.TIMED_OUT, second.getTerminalState());
        assertTrue(second.isTerminalAlreadyReached());
    }

    @Test
    public void abandonAll_marksPendingRequestsAndLateCallbacksBecomeUndeliverable() {
        BridgeRequestCoordinator coordinator = new BridgeRequestCoordinator(5_000L, new FakeClock(20_000L));
        coordinator.begin("corr-4", "requestCharge", "cb-4", "normal", "2026-04-08T10:00:03Z");

        List<InFlightRequestRecord> abandoned = coordinator.abandonAll();
        BridgeRequestCoordinator.Decision lateSuccess = coordinator.acceptSuccess("corr-4", 0);

        assertEquals(1, abandoned.size());
        assertEquals("abandoned", abandoned.get(0).getState());
        assertTrue(coordinator.snapshot().isEmpty());
        assertFalse(lateSuccess.isAccepted());
        assertEquals(BridgeRequestCoordinator.TerminalState.ABANDONED, lateSuccess.getTerminalState());
    }

    @Test
    public void missingRequest_returnsIgnoredDecision() {
        BridgeRequestCoordinator coordinator = new BridgeRequestCoordinator(5_000L, new FakeClock(30_000L));

        BridgeRequestCoordinator.Decision decision = coordinator.acceptError("missing", 1, SdkErrorCode.ERR_UNKNOWN);

        assertFalse(decision.isAccepted());
        assertNull(decision.getTerminalState());
        assertEquals("missing", decision.getReason());
    }

    private static final class FakeClock implements BridgeRequestCoordinator.Clock {
        private long nowMs;

        private FakeClock(long nowMs) {
            this.nowMs = nowMs;
        }

        @Override
        public long nowMs() {
            return nowMs;
        }

        void advance(long deltaMs) {
            nowMs += deltaMs;
        }
    }
}
