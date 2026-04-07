import Foundation

struct DiagnosticsSnapshot: Codable, Equatable {
    static let schemaVersion = 1

    let schemaVersion: Int
    let exportedAt: String
    let timelines: [RequestTimeline]

    init(exportedAt: String, timelines: [RequestTimeline]) {
        self.schemaVersion = DiagnosticsSnapshot.schemaVersion
        self.exportedAt = exportedAt
        self.timelines = timelines
    }
}

struct DiagnosticsPayload: Codable, Equatable {
    let activePreset: String
    let availablePresets: [String]
    let inFlightRequests: [InFlightRequestRecord]
    let snapshot: DiagnosticsSnapshot

    func jsonString(prettyPrinted: Bool = false) -> String {
        let encoder = JSONEncoder()
        if prettyPrinted {
            encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        }

        guard
            let data = try? encoder.encode(self),
            let value = String(data: data, encoding: .utf8)
        else {
            return "{}"
        }

        return value
    }
}
