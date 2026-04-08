package com.demo.railbridge.kotlin

import com.demo.railbridge.sdk.SdkErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinBridgeRequestCoordinatorTest {

    @Test
    fun begin_makesRequestVisibleInSnapshot() {
        val clock = FakeClock(1_000L)
        val coordinator = KotlinBridgeRequestCoordinator(5_000L, clock)

        coordinator.begin("corr-1", "requestCharge", "cb-1", "callback_loss", "2026-04-08T10:00:00Z")
        clock.advance(850L)
        val snapshot = coordinator.snapshot()

        assertEquals(1, snapshot.size)
        assertEquals("corr-1", snapshot[0].correlationId)
        assertEquals("requestCharge", snapshot[0].method)
        assertEquals("cb-1", snapshot[0].callbackId)
        assertEquals("pending", snapshot[0].state)
        assertEquals("callback_loss", snapshot[0].scenario)
        assertEquals(850L, snapshot[0].elapsedMs)
    }

    @Test
    fun acceptSuccess_suppressesDuplicateCallbacksAfterFirstTerminalOutcome() {
        val coordinator = KotlinBridgeRequestCoordinator(5_000L, FakeClock(2_000L))
        coordinator.begin("corr-2", "requestCharge", "cb-2", "duplicate_callback", "2026-04-08T10:00:01Z")

        val first = coordinator.acceptSuccess("corr-2", 0)
        val second = coordinator.acceptSuccess("corr-2", 0)

        assertTrue(first.accepted)
        assertEquals(KotlinBridgeRequestCoordinator.TerminalState.SUCCESS, first.terminalState)
        assertFalse(second.accepted)
        assertEquals(KotlinBridgeRequestCoordinator.TerminalState.SUCCESS, second.terminalState)
        assertTrue(second.terminalAlreadyReached)
    }

    @Test
    fun acceptTimeout_onlyFiresOnceAndOnlyAfterDeadline() {
        val clock = FakeClock(10_000L)
        val coordinator = KotlinBridgeRequestCoordinator(5_000L, clock)
        coordinator.begin("corr-3", "getBalance", null, "callback_loss", "2026-04-08T10:00:02Z")

        val early = coordinator.acceptTimeout("corr-3")
        clock.advance(5_000L)
        val first = coordinator.acceptTimeout("corr-3")
        val second = coordinator.acceptTimeout("corr-3")

        assertFalse(early.accepted)
        assertEquals("not_due", early.reason)
        assertEquals(KotlinBridgeRequestCoordinator.TerminalState.PENDING, early.terminalState)
        assertTrue(first.accepted)
        assertEquals(KotlinBridgeRequestCoordinator.TerminalState.TIMED_OUT, first.terminalState)
        assertFalse(second.accepted)
        assertEquals(KotlinBridgeRequestCoordinator.TerminalState.TIMED_OUT, second.terminalState)
        assertTrue(second.terminalAlreadyReached)
    }

    @Test
    fun abandonAll_marksPendingRequestsAndLateCallbacksBecomeUndeliverable() {
        val coordinator = KotlinBridgeRequestCoordinator(5_000L, FakeClock(20_000L))
        coordinator.begin("corr-4", "requestCharge", "cb-4", "normal", "2026-04-08T10:00:03Z")

        val abandoned = coordinator.abandonAll()
        val lateSuccess = coordinator.acceptSuccess("corr-4", 0)

        assertEquals(1, abandoned.size)
        assertEquals("abandoned", abandoned[0].state)
        assertTrue(coordinator.snapshot().isEmpty())
        assertFalse(lateSuccess.accepted)
        assertEquals(KotlinBridgeRequestCoordinator.TerminalState.ABANDONED, lateSuccess.terminalState)
    }

    @Test
    fun missingRequest_returnsIgnoredDecision() {
        val coordinator = KotlinBridgeRequestCoordinator(5_000L, FakeClock(30_000L))

        val decision = coordinator.acceptError("missing", 1, SdkErrorCode.ERR_UNKNOWN)

        assertFalse(decision.accepted)
        assertNull(decision.terminalState)
        assertEquals("missing", decision.reason)
    }

    private class FakeClock(private var nowMs: Long) : KotlinBridgeRequestCoordinator.Clock {
        override fun nowMs(): Long = nowMs

        fun advance(deltaMs: Long) {
            nowMs += deltaMs
        }
    }
}
