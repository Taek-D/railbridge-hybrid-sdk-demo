import Foundation

final class ErrorLogger {
    private let queue = DispatchQueue(label: "com.demo.railbridge.ios.error-logger")
    private let maxTimelines: Int
    private var timelines: [RequestTimeline] = []

    init(maxTimelines: Int) {
        self.maxTimelines = max(1, maxTimelines)
    }

    func record(_ event: TimelineEvent) {
        queue.sync {
            if let existingIndex = timelines.firstIndex(where: { $0.correlationId == event.correlationId }) {
                var timeline = timelines.remove(at: existingIndex)
                timeline.append(event)
                timelines.insert(timeline, at: 0)
            } else {
                var timeline = RequestTimeline(
                    correlationId: event.correlationId,
                    method: event.method,
                    scenario: event.scenario
                )
                timeline.append(event)
                timelines.insert(timeline, at: 0)
            }

            if timelines.count > maxTimelines {
                timelines = Array(timelines.prefix(maxTimelines))
            }
        }
    }

    func getDiagnosticsSnapshot(now: String = Timestamp.isoString()) -> DiagnosticsSnapshot {
        queue.sync {
            DiagnosticsSnapshot(exportedAt: now, timelines: timelines)
        }
    }

    func clear() {
        queue.sync {
            timelines.removeAll()
        }
    }
}

enum Timestamp {
    private static let formatter = ISO8601DateFormatter()

    static func isoString(from date: Date = Date()) -> String {
        formatter.string(from: date)
    }

    static func nowMs(from date: Date = Date()) -> Int64 {
        Int64(date.timeIntervalSince1970 * 1000)
    }
}
