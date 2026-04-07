import Foundation

struct RequestTimeline: Codable, Equatable {
    let correlationId: String
    private(set) var method: String
    private(set) var scenario: String
    private(set) var finalStatus: String = "incomplete"
    private(set) var resolvedByRetry: Bool = false
    private(set) var finalRetryCount: Int = 0
    private(set) var vendorCode: String?
    private(set) var retryable: Bool?
    private(set) var updatedAt: String?
    private(set) var events: [TimelineEvent] = []

    init(correlationId: String, method: String, scenario: String) {
        self.correlationId = correlationId
        self.method = method
        self.scenario = scenario
    }

    mutating func append(_ event: TimelineEvent) {
        events.append(event)

        if !event.method.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            method = event.method
        }
        if !event.scenario.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            scenario = event.scenario
        }
        if let vendorCode = event.vendorCode, !vendorCode.isEmpty {
            self.vendorCode = vendorCode
        }
        if let retryable = event.retryable {
            self.retryable = retryable
        }

        finalRetryCount = max(finalRetryCount, event.retryCount)
        updatedAt = event.timestamp

        if let finalStatus = event.finalStatus, !finalStatus.isEmpty {
            self.finalStatus = finalStatus
            if let resolvedByRetry = event.resolvedByRetry {
                self.resolvedByRetry = resolvedByRetry
            } else {
                self.resolvedByRetry = finalStatus == "success" && finalRetryCount > 0
            }
        }
    }
}
