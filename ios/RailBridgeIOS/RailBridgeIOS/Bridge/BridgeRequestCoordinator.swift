import Foundation

struct InFlightRequestRecord: Codable, Equatable {
    let correlationId: String
    let method: String
    let callbackId: String?
    let state: String
    let scenario: String
    let startedAt: String
    let elapsedMs: Int
}

final class BridgeRequestCoordinator {
    enum TerminalState: String, Codable {
        case pending = "pending"
        case success = "success"
        case error = "error"
        case timedOut = "timed_out"
        case abandoned = "abandoned"
    }

    struct RegisteredRequest: Equatable {
        let correlationId: String
        let timeoutAtMs: Int64
    }

    struct Decision: Equatable {
        let accepted: Bool
        let terminalState: TerminalState?
        let reason: String
        let terminalAlreadyReached: Bool
        let retryCount: Int

        static func accepted(_ terminalState: TerminalState, retryCount: Int) -> Decision {
            Decision(
                accepted: true,
                terminalState: terminalState,
                reason: "accepted",
                terminalAlreadyReached: false,
                retryCount: retryCount
            )
        }

        static func ignored(
            _ terminalState: TerminalState?,
            reason: String,
            terminalAlreadyReached: Bool,
            retryCount: Int
        ) -> Decision {
            Decision(
                accepted: false,
                terminalState: terminalState,
                reason: reason,
                terminalAlreadyReached: terminalAlreadyReached,
                retryCount: retryCount
            )
        }
    }

    private final class RequestState {
        let correlationId: String
        let method: String
        let callbackId: String?
        let scenario: String
        let startedAt: String
        let startedAtMs: Int64
        let timeoutAtMs: Int64

        var terminalState: TerminalState = .pending
        var retryCount = 0

        init(
            correlationId: String,
            method: String,
            callbackId: String?,
            scenario: String,
            startedAt: String,
            startedAtMs: Int64,
            timeoutAtMs: Int64
        ) {
            self.correlationId = correlationId
            self.method = method
            self.callbackId = callbackId
            self.scenario = scenario
            self.startedAt = startedAt
            self.startedAtMs = startedAtMs
            self.timeoutAtMs = timeoutAtMs
        }
    }

    private let queue = DispatchQueue(label: "com.demo.railbridge.ios.request-coordinator")
    private let timeoutMs: Int64
    private let nowProvider: () -> Int64
    private var requests: [String: RequestState] = [:]

    init(timeoutMs: Int64, nowProvider: @escaping () -> Int64 = { Timestamp.nowMs() }) {
        self.timeoutMs = max(1, timeoutMs)
        self.nowProvider = nowProvider
    }

    func begin(
        correlationId: String,
        method: String,
        callbackId: String?,
        scenario: String,
        startedAt: String
    ) -> RegisteredRequest {
        queue.sync {
            let startedAtMs = nowProvider()
            let state = RequestState(
                correlationId: correlationId,
                method: method,
                callbackId: callbackId,
                scenario: scenario,
                startedAt: startedAt,
                startedAtMs: startedAtMs,
                timeoutAtMs: startedAtMs + timeoutMs
            )
            requests[correlationId] = state
            return RegisteredRequest(correlationId: correlationId, timeoutAtMs: state.timeoutAtMs)
        }
    }

    func acceptSuccess(correlationId: String, retryCount: Int) -> Decision {
        transition(correlationId: correlationId, target: .success, retryCount: retryCount)
    }

    func acceptError(correlationId: String, retryCount: Int) -> Decision {
        transition(correlationId: correlationId, target: .error, retryCount: retryCount)
    }

    func acceptTimeout(correlationId: String) -> Decision {
        queue.sync {
            guard let request = requests[correlationId] else {
                return .ignored(nil, reason: "missing", terminalAlreadyReached: false, retryCount: 0)
            }

            guard request.terminalState == .pending else {
                return .ignored(
                    request.terminalState,
                    reason: "already_terminal",
                    terminalAlreadyReached: true,
                    retryCount: request.retryCount
                )
            }

            guard nowProvider() >= request.timeoutAtMs else {
                return .ignored(.pending, reason: "not_due", terminalAlreadyReached: false, retryCount: request.retryCount)
            }

            request.terminalState = .timedOut
            return .accepted(.timedOut, retryCount: request.retryCount)
        }
    }

    func abandonAll() -> [InFlightRequestRecord] {
        queue.sync {
            var abandoned: [InFlightRequestRecord] = []
            for request in requests.values where request.terminalState == .pending {
                request.terminalState = .abandoned
                abandoned.append(record(from: request))
            }
            return abandoned.sorted(by: { $0.startedAt < $1.startedAt })
        }
    }

    func snapshot() -> [InFlightRequestRecord] {
        queue.sync {
            requests.values
                .filter { $0.terminalState == .pending }
                .map(record(from:))
                .sorted(by: { $0.startedAt < $1.startedAt })
        }
    }

    private func transition(correlationId: String, target: TerminalState, retryCount: Int) -> Decision {
        queue.sync {
            guard let request = requests[correlationId] else {
                return .ignored(nil, reason: "missing", terminalAlreadyReached: false, retryCount: retryCount)
            }

            guard request.terminalState == .pending else {
                return .ignored(
                    request.terminalState,
                    reason: "already_terminal",
                    terminalAlreadyReached: true,
                    retryCount: request.retryCount
                )
            }

            request.terminalState = target
            request.retryCount = max(0, retryCount)
            return .accepted(target, retryCount: request.retryCount)
        }
    }

    private func record(from request: RequestState) -> InFlightRequestRecord {
        InFlightRequestRecord(
            correlationId: request.correlationId,
            method: request.method,
            callbackId: request.callbackId,
            state: request.terminalState.rawValue,
            scenario: request.scenario,
            startedAt: request.startedAt,
            elapsedMs: max(0, Int(nowProvider() - request.startedAtMs))
        )
    }
}
