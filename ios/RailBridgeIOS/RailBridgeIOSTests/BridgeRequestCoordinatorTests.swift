import XCTest
@testable import RailBridgeIOS

final class BridgeRequestCoordinatorTests: XCTestCase {
    func testDuplicateSuppressionAfterSuccess() {
        var now: Int64 = 1_000
        let coordinator = BridgeRequestCoordinator(timeoutMs: 5_000, nowProvider: { now })
        _ = coordinator.begin(
            correlationId: "corr-1",
            method: "requestCharge",
            callbackId: "cb-1",
            scenario: "duplicate_callback",
            startedAt: "2026-04-08T00:00:00Z"
        )

        let first = coordinator.acceptSuccess(correlationId: "corr-1", retryCount: 0)
        let second = coordinator.acceptError(correlationId: "corr-1", retryCount: 0)

        XCTAssertTrue(first.accepted)
        XCTAssertFalse(second.accepted)
        XCTAssertEqual(second.terminalState, .success)
        XCTAssertTrue(second.terminalAlreadyReached)
    }

    func testTimeoutOnlyFiresOnce() {
        var now: Int64 = 1_000
        let coordinator = BridgeRequestCoordinator(timeoutMs: 5_000, nowProvider: { now })
        _ = coordinator.begin(
            correlationId: "corr-2",
            method: "requestCharge",
            callbackId: nil,
            scenario: "callback_loss",
            startedAt: "2026-04-08T00:00:00Z"
        )

        now += 5_000
        let first = coordinator.acceptTimeout(correlationId: "corr-2")
        let second = coordinator.acceptTimeout(correlationId: "corr-2")

        XCTAssertTrue(first.accepted)
        XCTAssertEqual(first.terminalState, .timedOut)
        XCTAssertFalse(second.accepted)
        XCTAssertEqual(second.terminalState, .timedOut)
    }
}
