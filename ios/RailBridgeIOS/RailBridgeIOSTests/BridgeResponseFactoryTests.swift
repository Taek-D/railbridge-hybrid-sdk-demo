import XCTest
@testable import RailBridgeIOS

final class BridgeResponseFactoryTests: XCTestCase {
    func testSuccessPayloadKeepsAndroidAlignedFields() throws {
        let metadata = BridgeResponseFactory.Metadata(
            callbackId: "cb-1",
            correlationId: "corr-1",
            platform: "ios",
            stage: "js_callback",
            durationMs: 12,
            scenario: "normal",
            vendorCode: nil,
            retryable: nil,
            resolvedByRetry: nil
        )
        let payload = BridgeResponseFactory.buildChargeSuccess(
            ChargeResult(transactionId: "txn-1", amount: 10000, balance: 28947, timestamp: "2026-04-08T00:00:00Z"),
            retryCount: 1,
            metadata: metadata
        )

        let object = try XCTUnwrap(parse(payload))
        XCTAssertEqual(object["status"] as? String, "success")
        XCTAssertEqual(object["method"] as? String, "requestCharge")
        XCTAssertEqual(object["retryCount"] as? Int, 1)
        XCTAssertEqual(object["callbackId"] as? String, "cb-1")
        XCTAssertEqual(object["correlationId"] as? String, "corr-1")
        XCTAssertEqual(object["platform"] as? String, "ios")
        XCTAssertEqual(object["stage"] as? String, "js_callback")
        XCTAssertEqual(object["durationMs"] as? Int, 12)
    }

    func testErrorPayloadContainsRetryCountForTopLevelAndNestedError() throws {
        let metadata = BridgeResponseFactory.Metadata(
            callbackId: nil,
            correlationId: "corr-2",
            platform: "ios",
            stage: "js_callback",
            durationMs: 5020,
            scenario: "callback_loss",
            vendorCode: "VENDOR_TIMEOUT",
            retryable: false,
            resolvedByRetry: false
        )
        let payload = BridgeResponseFactory.buildError(
            method: "getBalance",
            errorCode: .timeout,
            retryCount: 3,
            logId: "log-1",
            vendorCode: "VENDOR_TIMEOUT",
            retryable: false,
            metadata: metadata
        )

        let object = try XCTUnwrap(parse(payload))
        XCTAssertEqual(object["status"] as? String, "error")
        XCTAssertEqual(object["method"] as? String, "getBalance")
        XCTAssertEqual(object["retryCount"] as? Int, 3)
        XCTAssertEqual(object["vendorCode"] as? String, "VENDOR_TIMEOUT")
        XCTAssertEqual(object["retryable"] as? Bool, false)

        let error = try XCTUnwrap(object["error"] as? [String: Any])
        XCTAssertEqual(error["code"] as? String, "1005")
        XCTAssertEqual(error["retryCount"] as? Int, 3)
    }

    private func parse(_ payload: String) -> [String: Any]? {
        guard let data = payload.data(using: .utf8) else {
            return nil
        }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }
}
