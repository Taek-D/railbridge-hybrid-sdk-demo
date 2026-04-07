import XCTest
@testable import RailBridgeIOS

final class DiagnosticsSnapshotTests: XCTestCase {
    func testDiagnosticsPayloadContainsSchemaVersionAndInFlightRequests() {
        let timelineEvent = TimelineEvent.stage(
            correlationId: "corr-1",
            method: "requestCharge",
            scenario: "normal",
            stage: "js_entry",
            timestamp: "2026-04-08T00:00:00Z",
            retryCount: 0,
            durationMs: 0,
            vendorCode: nil,
            retryable: nil
        )
        var timeline = RequestTimeline(correlationId: "corr-1", method: "requestCharge", scenario: "normal")
        timeline.append(timelineEvent)

        let payload = DiagnosticsPayload(
            activePreset: "normal",
            availablePresets: ScenarioPreset.allValues,
            inFlightRequests: [
                InFlightRequestRecord(
                    correlationId: "corr-1",
                    method: "requestCharge",
                    callbackId: "cb-1",
                    state: "pending",
                    scenario: "normal",
                    startedAt: "2026-04-08T00:00:00Z",
                    elapsedMs: 120
                )
            ],
            snapshot: DiagnosticsSnapshot(exportedAt: "2026-04-08T00:00:01Z", timelines: [timeline])
        )

        let json = payload.jsonString()
        XCTAssertTrue(json.contains("\"schemaVersion\":1"))
        XCTAssertTrue(json.contains("\"inFlightRequests\""))
        XCTAssertTrue(json.contains("\"timelines\""))
    }
}
