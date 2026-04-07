import Foundation

enum ScenarioPreset: String, CaseIterable, Codable {
    case normal = "normal"
    case timeout = "timeout"
    case internalError = "internal_error"
    case callbackLoss = "callback_loss"
    case duplicateCallback = "duplicate_callback"
    case retryExhausted = "retry_exhausted"

    static func from(_ rawValue: String?) -> ScenarioPreset {
        guard let rawValue else {
            return .normal
        }

        let normalized = rawValue
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "-", with: "_")
            .replacingOccurrences(of: " ", with: "_")

        return ScenarioPreset(rawValue: normalized) ?? .normal
    }

    static var allValues: [String] {
        allCases.map(\.rawValue)
    }
}
