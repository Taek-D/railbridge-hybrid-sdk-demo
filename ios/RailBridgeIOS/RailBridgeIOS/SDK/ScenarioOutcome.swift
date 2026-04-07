import Foundation

struct ScenarioOutcome: Equatable {
    let emitsCallback: Bool
    let usesBackendSuccessPath: Bool
    let duplicateCallback: Bool
    let clearAttemptsAfterHandling: Bool
    let errorCode: SdkErrorCode?
    let vendorCode: String?
    let retryable: Bool

    static func forAttempt(_ preset: ScenarioPreset, attempt: Int) -> ScenarioOutcome {
        let safeAttempt = max(1, attempt)

        switch preset {
        case .timeout:
            if safeAttempt == 1 {
                return failure(
                    errorCode: .networkTimeout,
                    vendorCode: "VENDOR_TIMEOUT",
                    retryable: true,
                    clearAttemptsAfterHandling: false
                )
            }
            return backendSuccess(duplicateCallback: false)
        case .internalError:
            return failure(
                errorCode: .vendorInternal,
                vendorCode: "VENDOR_INTERNAL",
                retryable: false,
                clearAttemptsAfterHandling: true
            )
        case .callbackLoss:
            return ScenarioOutcome(
                emitsCallback: false,
                usesBackendSuccessPath: false,
                duplicateCallback: false,
                clearAttemptsAfterHandling: true,
                errorCode: nil,
                vendorCode: "CALLBACK_LOSS",
                retryable: false
            )
        case .duplicateCallback:
            return backendSuccess(duplicateCallback: true)
        case .retryExhausted:
            return failure(
                errorCode: .networkTimeout,
                vendorCode: "VENDOR_TIMEOUT",
                retryable: true,
                clearAttemptsAfterHandling: true
            )
        case .normal:
            return backendSuccess(duplicateCallback: false)
        }
    }

    private static func backendSuccess(duplicateCallback: Bool) -> ScenarioOutcome {
        ScenarioOutcome(
            emitsCallback: true,
            usesBackendSuccessPath: true,
            duplicateCallback: duplicateCallback,
            clearAttemptsAfterHandling: true,
            errorCode: nil,
            vendorCode: nil,
            retryable: false
        )
    }

    private static func failure(
        errorCode: SdkErrorCode,
        vendorCode: String,
        retryable: Bool,
        clearAttemptsAfterHandling: Bool
    ) -> ScenarioOutcome {
        ScenarioOutcome(
            emitsCallback: true,
            usesBackendSuccessPath: false,
            duplicateCallback: false,
            clearAttemptsAfterHandling: clearAttemptsAfterHandling,
            errorCode: errorCode,
            vendorCode: vendorCode,
            retryable: retryable
        )
    }
}
