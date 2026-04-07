package com.demo.railbridge.bridge;

import com.demo.railbridge.sdk.SdkErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BridgeRequestCoordinator {

    private final Map<String, RequestState> requests = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long timeoutMs;

    public BridgeRequestCoordinator(long timeoutMs) {
        this(timeoutMs, Clock.system());
    }

    BridgeRequestCoordinator(long timeoutMs, Clock clock) {
        this.timeoutMs = Math.max(1L, timeoutMs);
        this.clock = clock == null ? Clock.system() : clock;
    }

    public RegisteredRequest begin(
            String correlationId,
            String method,
            String callbackId,
            String scenario,
            String startedAt
    ) {
        RequestState state = new RequestState(
                correlationId,
                method,
                callbackId,
                scenario,
                startedAt,
                clock.nowMs(),
                clock.nowMs() + timeoutMs
        );
        requests.put(correlationId, state);
        return new RegisteredRequest(correlationId, state.timeoutAtMs);
    }

    public Decision acceptSuccess(String correlationId, int retryCount) {
        return transition(correlationId, TerminalState.SUCCESS, retryCount, null);
    }

    public Decision acceptError(String correlationId, int retryCount, SdkErrorCode errorCode) {
        return transition(correlationId, TerminalState.ERROR, retryCount, errorCode);
    }

    public Decision acceptTimeout(String correlationId) {
        RequestState requestState = requests.get(correlationId);
        if (requestState == null) {
            return Decision.ignored(null, "missing", false, 0);
        }
        synchronized (requestState) {
            if (requestState.terminalState != TerminalState.PENDING) {
                return Decision.ignored(requestState.terminalState, "already_terminal", true, requestState.retryCount);
            }
            if (clock.nowMs() < requestState.timeoutAtMs) {
                return Decision.ignored(TerminalState.PENDING, "not_due", false, requestState.retryCount);
            }
            requestState.terminalState = TerminalState.TIMED_OUT;
            requestState.retryCount = Math.max(0, requestState.retryCount);
            return Decision.accepted(TerminalState.TIMED_OUT, requestState.retryCount);
        }
    }

    public List<InFlightRequestRecord> abandonAll() {
        List<InFlightRequestRecord> abandoned = new ArrayList<>();
        for (RequestState requestState : requests.values()) {
            synchronized (requestState) {
                if (requestState.terminalState == TerminalState.PENDING) {
                    requestState.terminalState = TerminalState.ABANDONED;
                    abandoned.add(toRecord(requestState));
                }
            }
        }
        sortByStartedAt(abandoned);
        return abandoned;
    }

    public List<InFlightRequestRecord> snapshot() {
        List<InFlightRequestRecord> inFlight = new ArrayList<>();
        for (RequestState requestState : requests.values()) {
            synchronized (requestState) {
                if (requestState.terminalState == TerminalState.PENDING) {
                    inFlight.add(toRecord(requestState));
                }
            }
        }
        sortByStartedAt(inFlight);
        return inFlight;
    }

    private Decision transition(
            String correlationId,
            TerminalState target,
            int retryCount,
            SdkErrorCode errorCode
    ) {
        RequestState requestState = requests.get(correlationId);
        if (requestState == null) {
            return Decision.ignored(null, "missing", false, retryCount);
        }

        synchronized (requestState) {
            if (requestState.terminalState != TerminalState.PENDING) {
                return Decision.ignored(
                        requestState.terminalState,
                        "already_terminal",
                        true,
                        requestState.retryCount
                );
            }
            requestState.terminalState = target;
            requestState.retryCount = Math.max(0, retryCount);
            requestState.lastErrorCode = errorCode;
            return Decision.accepted(target, requestState.retryCount);
        }
    }

    private InFlightRequestRecord toRecord(RequestState requestState) {
        return new InFlightRequestRecord(
                requestState.correlationId,
                requestState.method,
                requestState.callbackId,
                requestState.terminalState.getValue(),
                requestState.scenario,
                requestState.startedAt,
                Math.max(0L, clock.nowMs() - requestState.startedAtMs)
        );
    }

    private static void sortByStartedAt(List<InFlightRequestRecord> records) {
        Collections.sort(records, new Comparator<InFlightRequestRecord>() {
            @Override
            public int compare(InFlightRequestRecord left, InFlightRequestRecord right) {
                return left.getStartedAt().compareTo(right.getStartedAt());
            }
        });
    }

    interface Clock {
        long nowMs();

        static Clock system() {
            return new Clock() {
                @Override
                public long nowMs() {
                    return System.currentTimeMillis();
                }
            };
        }
    }

    public static final class RegisteredRequest {
        private final String correlationId;
        private final long timeoutAtMs;

        private RegisteredRequest(String correlationId, long timeoutAtMs) {
            this.correlationId = correlationId;
            this.timeoutAtMs = timeoutAtMs;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public long getTimeoutAtMs() {
            return timeoutAtMs;
        }
    }

    public static final class Decision {
        private final boolean accepted;
        private final TerminalState terminalState;
        private final String reason;
        private final boolean terminalAlreadyReached;
        private final int retryCount;

        private Decision(
                boolean accepted,
                TerminalState terminalState,
                String reason,
                boolean terminalAlreadyReached,
                int retryCount
        ) {
            this.accepted = accepted;
            this.terminalState = terminalState;
            this.reason = reason;
            this.terminalAlreadyReached = terminalAlreadyReached;
            this.retryCount = retryCount;
        }

        static Decision accepted(TerminalState terminalState, int retryCount) {
            return new Decision(true, terminalState, "accepted", false, retryCount);
        }

        static Decision ignored(
                TerminalState terminalState,
                String reason,
                boolean terminalAlreadyReached,
                int retryCount
        ) {
            return new Decision(false, terminalState, reason, terminalAlreadyReached, retryCount);
        }

        public boolean isAccepted() {
            return accepted;
        }

        public TerminalState getTerminalState() {
            return terminalState;
        }

        public String getReason() {
            return reason;
        }

        public boolean isTerminalAlreadyReached() {
            return terminalAlreadyReached;
        }

        public int getRetryCount() {
            return retryCount;
        }
    }

    public enum TerminalState {
        PENDING("pending"),
        SUCCESS("success"),
        ERROR("error"),
        TIMED_OUT("timed_out"),
        ABANDONED("abandoned");

        private final String value;

        TerminalState(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static final class RequestState {
        private final String correlationId;
        private final String method;
        private final String callbackId;
        private final String scenario;
        private final String startedAt;
        private final long startedAtMs;
        private final long timeoutAtMs;

        private TerminalState terminalState = TerminalState.PENDING;
        private int retryCount;
        private SdkErrorCode lastErrorCode;

        private RequestState(
                String correlationId,
                String method,
                String callbackId,
                String scenario,
                String startedAt,
                long startedAtMs,
                long timeoutAtMs
        ) {
            this.correlationId = correlationId;
            this.method = method;
            this.callbackId = callbackId;
            this.scenario = scenario;
            this.startedAt = startedAt;
            this.startedAtMs = startedAtMs;
            this.timeoutAtMs = timeoutAtMs;
        }
    }
}
