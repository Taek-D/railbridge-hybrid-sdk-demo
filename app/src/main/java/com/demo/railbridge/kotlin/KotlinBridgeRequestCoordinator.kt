package com.demo.railbridge.kotlin

import com.demo.railbridge.bridge.InFlightRequestRecord
import com.demo.railbridge.sdk.SdkErrorCode
import java.util.concurrent.ConcurrentHashMap

class KotlinBridgeRequestCoordinator(
    timeoutMs: Long,
    private val clock: Clock = Clock.system()
) {
    private val requests = ConcurrentHashMap<String, RequestState>()
    private val timeoutMs = timeoutMs.coerceAtLeast(1L)

    fun begin(
        correlationId: String,
        method: String,
        callbackId: String?,
        scenario: String,
        startedAt: String
    ): RegisteredRequest {
        val nowMs = clock.nowMs()
        requests[correlationId] = RequestState(
            correlationId = correlationId,
            method = method,
            callbackId = callbackId,
            scenario = scenario,
            startedAt = startedAt,
            startedAtMs = nowMs,
            timeoutAtMs = nowMs + timeoutMs
        )
        return RegisteredRequest(correlationId, nowMs + timeoutMs)
    }

    fun acceptSuccess(correlationId: String, retryCount: Int): Decision {
        return transition(correlationId, TerminalState.SUCCESS, retryCount, null)
    }

    fun acceptError(correlationId: String, retryCount: Int, errorCode: SdkErrorCode?): Decision {
        return transition(correlationId, TerminalState.ERROR, retryCount, errorCode)
    }

    fun acceptTimeout(correlationId: String): Decision {
        val state = requests[correlationId] ?: return Decision.ignored(null, "missing", false, 0)
        synchronized(state) {
            if (state.terminalState != TerminalState.PENDING) {
                return Decision.ignored(state.terminalState, "already_terminal", true, state.retryCount)
            }
            if (clock.nowMs() < state.timeoutAtMs) {
                return Decision.ignored(TerminalState.PENDING, "not_due", false, state.retryCount)
            }
            state.terminalState = TerminalState.TIMED_OUT
            state.retryCount = state.retryCount.coerceAtLeast(0)
            return Decision.accepted(TerminalState.TIMED_OUT, state.retryCount)
        }
    }

    fun abandonAll(): List<InFlightRequestRecord> {
        val abandoned = mutableListOf<InFlightRequestRecord>()
        for (state in requests.values) {
            synchronized(state) {
                if (state.terminalState == TerminalState.PENDING) {
                    state.terminalState = TerminalState.ABANDONED
                    abandoned += state.toRecord(clock.nowMs())
                }
            }
        }
        return abandoned.sortedBy { it.startedAt }
    }

    fun snapshot(): List<InFlightRequestRecord> {
        val inFlight = mutableListOf<InFlightRequestRecord>()
        for (state in requests.values) {
            synchronized(state) {
                if (state.terminalState == TerminalState.PENDING) {
                    inFlight += state.toRecord(clock.nowMs())
                }
            }
        }
        return inFlight.sortedBy { it.startedAt }
    }

    private fun transition(
        correlationId: String,
        target: TerminalState,
        retryCount: Int,
        errorCode: SdkErrorCode?
    ): Decision {
        val state = requests[correlationId] ?: return Decision.ignored(null, "missing", false, retryCount)
        synchronized(state) {
            if (state.terminalState != TerminalState.PENDING) {
                return Decision.ignored(state.terminalState, "already_terminal", true, state.retryCount)
            }
            state.terminalState = target
            state.retryCount = retryCount.coerceAtLeast(0)
            state.lastErrorCode = errorCode
            return Decision.accepted(target, state.retryCount)
        }
    }

    interface Clock {
        fun nowMs(): Long

        companion object {
            fun system(): Clock = object : Clock {
                override fun nowMs(): Long = System.currentTimeMillis()
            }
        }
    }

    data class RegisteredRequest(
        val correlationId: String,
        val timeoutAtMs: Long
    )

    data class Decision(
        val accepted: Boolean,
        val terminalState: TerminalState?,
        val reason: String,
        val terminalAlreadyReached: Boolean,
        val retryCount: Int
    ) {
        companion object {
            fun accepted(terminalState: TerminalState, retryCount: Int): Decision {
                return Decision(true, terminalState, "accepted", false, retryCount)
            }

            fun ignored(
                terminalState: TerminalState?,
                reason: String,
                terminalAlreadyReached: Boolean,
                retryCount: Int
            ): Decision {
                return Decision(false, terminalState, reason, terminalAlreadyReached, retryCount)
            }
        }
    }

    enum class TerminalState(val value: String) {
        PENDING("pending"),
        SUCCESS("success"),
        ERROR("error"),
        TIMED_OUT("timed_out"),
        ABANDONED("abandoned")
    }

    private data class RequestState(
        val correlationId: String,
        val method: String,
        val callbackId: String?,
        val scenario: String,
        val startedAt: String,
        val startedAtMs: Long,
        val timeoutAtMs: Long,
        var terminalState: TerminalState = TerminalState.PENDING,
        var retryCount: Int = 0,
        var lastErrorCode: SdkErrorCode? = null
    ) {
        fun toRecord(nowMs: Long): InFlightRequestRecord {
            return InFlightRequestRecord(
                correlationId,
                method,
                callbackId,
                terminalState.value,
                scenario,
                startedAt,
                (nowMs - startedAtMs).coerceAtLeast(0L)
            )
        }
    }
}
