import Foundation

enum SdkErrorCode: String, Error, Codable, Equatable {
    case sdkInternal
    case networkTimeout
    case insufficientBalance
    case invalidCard
    case timeout
    case vendorInternal
    case unknown
    case retryExhausted

    var code: String {
        switch self {
        case .sdkInternal:
            return "1001"
        case .networkTimeout:
            return "1002"
        case .insufficientBalance:
            return "1003"
        case .invalidCard:
            return "1004"
        case .timeout:
            return "1005"
        case .vendorInternal:
            return "1006"
        case .unknown:
            return "9999"
        case .retryExhausted:
            return "9001"
        }
    }

    var message: String {
        switch self {
        case .sdkInternal:
            return "SDK internal error"
        case .networkTimeout:
            return "Network timeout"
        case .insufficientBalance:
            return "Insufficient balance"
        case .invalidCard:
            return "Invalid card"
        case .timeout:
            return "Request timeout"
        case .vendorInternal:
            return "Vendor internal error"
        case .unknown:
            return "Unknown error"
        case .retryExhausted:
            return "Retry attempts exhausted"
        }
    }

    var retryable: Bool {
        self == .networkTimeout || self == .sdkInternal
    }
}

protocol RailPlusSdkAdapter {
    func initialize(completion: @escaping (Bool) -> Void)
    func requestCharge(cardId: String, amount: Int, completion: @escaping (Result<ChargeResult, SdkErrorCode>) -> Void)
    func getBalance(cardId: String, completion: @escaping (Result<BalanceSnapshot, SdkErrorCode>) -> Void)
    func getStatus() -> SdkStatusSnapshot
    func shutdown()
}
