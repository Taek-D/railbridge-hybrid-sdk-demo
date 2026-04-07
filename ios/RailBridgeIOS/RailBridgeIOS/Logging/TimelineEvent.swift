import Foundation

struct TimelineEvent: Codable, Equatable {
    let correlationId: String
    let method: String
    let scenario: String
    let stage: String
    let timestamp: String
    let retryCount: Int
    let durationMs: Int?
    let vendorCode: String?
    let retryable: Bool?
    let finalStatus: String?
    let resolvedByRetry: Bool?

    static func stage(
        correlationId: String,
        method: String,
        scenario: String,
        stage: String,
        timestamp: String,
        retryCount: Int,
        durationMs: Int?,
        vendorCode: String?,
        retryable: Bool?
    ) -> TimelineEvent {
        TimelineEvent(
            correlationId: correlationId,
            method: method,
            scenario: scenario,
            stage: stage,
            timestamp: timestamp,
            retryCount: max(0, retryCount),
            durationMs: durationMs,
            vendorCode: vendorCode,
            retryable: retryable,
            finalStatus: nil,
            resolvedByRetry: nil
        )
    }

    func withCompletion(finalStatus: String, resolvedByRetry: Bool) -> TimelineEvent {
        TimelineEvent(
            correlationId: correlationId,
            method: method,
            scenario: scenario,
            stage: stage,
            timestamp: timestamp,
            retryCount: retryCount,
            durationMs: durationMs,
            vendorCode: vendorCode,
            retryable: retryable,
            finalStatus: finalStatus,
            resolvedByRetry: resolvedByRetry
        )
    }
}
