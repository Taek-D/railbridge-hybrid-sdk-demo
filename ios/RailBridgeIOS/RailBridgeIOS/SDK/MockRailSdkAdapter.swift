import Foundation

final class MockRailSdkAdapter: RailPlusSdkAdapter {
    private let queue = DispatchQueue(label: "com.demo.railbridge.ios.mock-sdk", qos: .userInitiated)
    private let scenarioController: ScenarioController
    private var requestAttempts: [String: Int] = [:]
    private var initialized = false

    init(scenarioController: ScenarioController = ScenarioController()) {
        self.scenarioController = scenarioController
    }

    func initialize(completion: @escaping (Bool) -> Void) {
        queue.asyncAfter(deadline: .now() + 0.05) { [weak self] in
            self?.initialized = true
            completion(true)
        }
    }

    func requestCharge(
        cardId: String,
        amount: Int,
        completion: @escaping (Result<ChargeResult, SdkErrorCode>) -> Void
    ) {
        handleScenarioAwareRequest(
            requestKey: buildRequestKey(method: "requestCharge", cardId: cardId, amount: amount),
            backend: { callback in
                self.queue.asyncAfter(deadline: .now() + 0.15) {
                    let result = ChargeResult(
                        transactionId: "IOS-TXN-\(Int(Date().timeIntervalSince1970 * 1000))",
                        amount: amount,
                        balance: max(0, 38947 - amount),
                        timestamp: ISO8601DateFormatter().string(from: Date())
                    )
                    callback(.success(result))
                }
            },
            completion: completion
        )
    }

    func getBalance(
        cardId: String,
        completion: @escaping (Result<BalanceSnapshot, SdkErrorCode>) -> Void
    ) {
        handleScenarioAwareRequest(
            requestKey: buildRequestKey(method: "getBalance", cardId: cardId, amount: nil),
            backend: { callback in
                self.queue.asyncAfter(deadline: .now() + 0.12) {
                    callback(.success(BalanceSnapshot(
                        cardId: cardId,
                        balance: 38947,
                        timestamp: ISO8601DateFormatter().string(from: Date())
                    )))
                }
            },
            completion: completion
        )
    }

    func getStatus() -> SdkStatusSnapshot {
        SdkStatusSnapshot(initialized: initialized, version: "1.2.3-mock")
    }

    func shutdown() {
        initialized = false
    }

    private func handleScenarioAwareRequest<T>(
        requestKey: String,
        backend: @escaping (@escaping (Result<T, SdkErrorCode>) -> Void) -> Void,
        completion: @escaping (Result<T, SdkErrorCode>) -> Void
    ) {
        let outcome = nextOutcome(for: requestKey)

        if !outcome.emitsCallback {
            if outcome.clearAttemptsAfterHandling {
                clearAttempts(for: requestKey)
            }
            return
        }

        if !outcome.usesBackendSuccessPath {
            let errorCode = outcome.errorCode ?? .unknown
            queue.asyncAfter(deadline: .now() + 0.15) {
                completion(.failure(errorCode))
                if outcome.clearAttemptsAfterHandling {
                    self.clearAttempts(for: requestKey)
                }
            }
            return
        }

        backend { result in
            switch result {
            case .success(let value):
                completion(.success(value))
                if outcome.duplicateCallback {
                    completion(.success(value))
                }
            case .failure(let errorCode):
                completion(.failure(errorCode))
            }

            self.clearAttempts(for: requestKey)
        }
    }

    private func nextOutcome(for requestKey: String) -> ScenarioOutcome {
        let attempt = queue.sync { () -> Int in
            let nextValue = (requestAttempts[requestKey] ?? 0) + 1
            requestAttempts[requestKey] = nextValue
            return nextValue
        }
        return ScenarioOutcome.forAttempt(scenarioController.activePreset, attempt: attempt)
    }

    private func clearAttempts(for requestKey: String) {
        queue.async {
            self.requestAttempts.removeValue(forKey: requestKey)
        }
    }

    private func buildRequestKey(method: String, cardId: String, amount: Int?) -> String {
        var key = "\(method)|\(cardId.trimmingCharacters(in: .whitespacesAndNewlines))"
        if let amount {
            key += "|\(amount)"
        }
        return key
    }
}
