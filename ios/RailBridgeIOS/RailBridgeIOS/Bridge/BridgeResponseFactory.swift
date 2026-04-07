import Foundation

enum BridgeResponseFactory {
    struct Metadata: Equatable {
        let callbackId: String?
        let correlationId: String?
        let platform: String?
        let stage: String?
        let durationMs: Int?
        let scenario: String?
        let vendorCode: String?
        let retryable: Bool?
        let resolvedByRetry: Bool?
    }

    static func buildChargeSuccess(
        _ result: ChargeResult,
        retryCount: Int = 0,
        metadata: Metadata?
    ) -> String {
        let data: [String: Any] = [
            "transactionId": result.transactionId,
            "amount": result.amount,
            "balance": result.balance,
            "timestamp": result.timestamp
        ]
        return buildSuccess(method: "requestCharge", data: data, retryCount: retryCount, metadata: metadata)
    }

    static func buildBalanceSuccess(
        _ result: BalanceSnapshot,
        retryCount: Int = 0,
        metadata: Metadata?
    ) -> String {
        let data: [String: Any] = [
            "cardId": result.cardId,
            "balance": result.balance,
            "timestamp": result.timestamp
        ]
        return buildSuccess(method: "getBalance", data: data, retryCount: retryCount, metadata: metadata)
    }

    static func buildStatusSuccess(_ result: SdkStatusSnapshot, metadata: Metadata?) -> String {
        let data: [String: Any] = [
            "initialized": result.initialized,
            "version": result.version
        ]
        return buildSuccess(method: "getSdkStatus", data: data, retryCount: 0, metadata: metadata)
    }

    static func buildReportAck(code: String, message: String, metadata: Metadata?) -> String {
        var response: [String: Any] = [
            "status": "received",
            "message": message,
            "code": code
        ]
        mergeMetadata(into: &response, metadata: metadata)
        return stringify(response)
    }

    static func buildError(
        method: String,
        errorCode: SdkErrorCode,
        retryCount: Int,
        logId: String?,
        vendorCode: String?,
        retryable: Bool?,
        metadata: Metadata?
    ) -> String {
        var error: [String: Any] = [
            "code": errorCode.code,
            "message": errorCode.message,
            "retryCount": retryCount,
            "resolved": false
        ]
        if let vendorCode, !vendorCode.isEmpty {
            error["vendorCode"] = vendorCode
        }
        if let retryable {
            error["retryable"] = retryable
        }

        var response: [String: Any] = [
            "status": "error",
            "method": method,
            "error": error,
            "retryCount": retryCount
        ]
        if let logId, !logId.isEmpty {
            response["logId"] = logId
        }
        mergeMetadata(into: &response, metadata: metadata)
        return stringify(response)
    }

    private static func buildSuccess(
        method: String,
        data: [String: Any],
        retryCount: Int,
        metadata: Metadata?
    ) -> String {
        var response: [String: Any] = [
            "status": "success",
            "method": method,
            "data": data,
            "retryCount": retryCount
        ]
        mergeMetadata(into: &response, metadata: metadata)
        return stringify(response)
    }

    private static func mergeMetadata(into response: inout [String: Any], metadata: Metadata?) {
        guard let metadata else {
            return
        }

        response["callbackId"] = metadata.callbackId
        response["correlationId"] = metadata.correlationId
        response["platform"] = metadata.platform
        response["stage"] = metadata.stage
        response["durationMs"] = metadata.durationMs
        response["scenario"] = metadata.scenario
        response["vendorCode"] = metadata.vendorCode
        response["retryable"] = metadata.retryable
        response["resolvedByRetry"] = metadata.resolvedByRetry
        response = response.filter { _, value in !(value is NSNull) && !isNilOptional(value) }
    }

    private static func isNilOptional(_ value: Any) -> Bool {
        let mirror = Mirror(reflecting: value)
        return mirror.displayStyle == .optional && mirror.children.isEmpty
    }

    private static func stringify(_ dictionary: [String: Any]) -> String {
        guard
            JSONSerialization.isValidJSONObject(dictionary),
            let data = try? JSONSerialization.data(withJSONObject: dictionary, options: []),
            let string = String(data: data, encoding: .utf8)
        else {
            return "{}"
        }

        return string
    }
}
