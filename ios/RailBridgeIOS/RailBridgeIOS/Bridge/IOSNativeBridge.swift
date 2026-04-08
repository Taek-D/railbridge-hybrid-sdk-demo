import Foundation
import WebKit

final class IOSNativeBridge: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
    private static let platform = "ios"
    private static let timeoutMs: Int64 = 5000
    private static let maxRetry = 3
    private static let backoffDelays: [Double] = [0.5, 1.0, 2.0]

    private let adapter: RailPlusSdkAdapter
    private let errorLogger: ErrorLogger
    private let scenarioController: ScenarioController
    private let requestCoordinator = BridgeRequestCoordinator(timeoutMs: IOSNativeBridge.timeoutMs)
    private let callbackQueue = DispatchQueue.main
    private let automation = AutomationConfig.current

    private weak var webView: WKWebView?
    private var timeoutWorkItems: [String: DispatchWorkItem] = [:]
    private var destroyed = false
    private var didRunAutomation = false

    init(
        adapter: RailPlusSdkAdapter,
        errorLogger: ErrorLogger,
        scenarioController: ScenarioController
    ) {
        self.adapter = adapter
        self.errorLogger = errorLogger
        self.scenarioController = scenarioController
        super.init()
    }

    func makeWebView() -> WKWebView {
        if let webView {
            return webView
        }

        let contentController = WKUserContentController()
        contentController.addUserScript(WKUserScript(
            source: injectedBridgeScript(),
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        ))
        contentController.add(self, name: "railBridge")

        let configuration = WKWebViewConfiguration()
        configuration.userContentController = contentController

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = self
        webView.allowsBackForwardNavigationGestures = false
        self.webView = webView
        refreshDiagnosticsCache()
        return webView
    }

    func destroy() {
        destroyed = true

        timeoutWorkItems.values.forEach { $0.cancel() }
        timeoutWorkItems.removeAll()

        for abandoned in requestCoordinator.abandonAll() {
            let event = TimelineEvent.stage(
                correlationId: abandoned.correlationId,
                method: abandoned.method,
                scenario: abandoned.scenario,
                stage: "bridge_abandoned",
                timestamp: Timestamp.isoString(),
                retryCount: 0,
                durationMs: abandoned.elapsedMs,
                vendorCode: nil,
                retryable: nil
            )
            errorLogger.record(event)
        }

        adapter.shutdown()
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: "railBridge")
        webView?.navigationDelegate = nil
        webView = nil
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        refreshDiagnosticsCache()
        runAutomationIfNeeded(on: webView)
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "railBridge", !destroyed else {
            return
        }

        guard let body = message.body as? [String: Any], let method = body["method"] as? String else {
            return
        }

        let params = body["params"] as? String

        switch method {
        case "requestCharge":
            requestCharge(paramsJson: params ?? "{}")
        case "getBalance":
            getBalance(paramsJson: params ?? "{}")
        case "getSdkStatus":
            getSdkStatus()
        case "reportError":
            reportError(paramsJson: params ?? "{}")
        case "setScenarioPreset":
            setScenarioPreset(params ?? ScenarioPreset.normal.rawValue)
        case "clearDiagnostics":
            clearDiagnostics()
        default:
            break
        }
    }

    private func requestCharge(paramsJson: String) {
        let scenario = scenarioController.activePreset.rawValue
        let params = parseParams(paramsJson)
        let cardId = params["cardId"] as? String ?? ""
        let amount = params["amount"] as? Int ?? Int((params["amount"] as? NSNumber)?.intValue ?? 0)
        let context = RequestContext(method: "requestCharge", callbackId: params["callbackId"] as? String, scenario: scenario)

        recordStage(context, stage: "js_entry", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        recordStage(context, stage: "native_validation", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        beginTrackedRequest(context)
        executeChargeAttempt(cardId: cardId, amount: amount, context: context, attempt: 1)
    }

    private func getBalance(paramsJson: String) {
        let scenario = scenarioController.activePreset.rawValue
        let params = parseParams(paramsJson)
        let cardId = params["cardId"] as? String ?? ""
        let context = RequestContext(method: "getBalance", callbackId: params["callbackId"] as? String, scenario: scenario)

        recordStage(context, stage: "js_entry", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        recordStage(context, stage: "native_validation", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        beginTrackedRequest(context)
        executeBalanceAttempt(cardId: cardId, context: context, attempt: 1)
    }

    private func getSdkStatus() {
        let context = RequestContext(method: "getSdkStatus", callbackId: nil, scenario: scenarioController.activePreset.rawValue)
        recordStage(context, stage: "js_entry", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        recordStage(context, stage: "native_validation", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)

        let metadata = buildMetadata(context: context, stage: "js_callback", vendorCode: nil, retryable: nil, resolvedByRetry: false)
        let resultJson = BridgeResponseFactory.buildStatusSuccess(adapter.getStatus(), metadata: metadata)
        postResultToJs(resultJson, context: context, retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: "success", resolvedByRetry: false)
    }

    private func reportError(paramsJson: String) {
        let context = RequestContext(method: "reportError", callbackId: nil, scenario: scenarioController.activePreset.rawValue)
        recordStage(context, stage: "js_entry", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        recordStage(context, stage: "native_validation", retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)

        let params = parseParams(paramsJson)
        let code = params["code"] as? String ?? "UNKNOWN"
        let metadata = buildMetadata(context: context, stage: "js_callback", vendorCode: nil, retryable: nil, resolvedByRetry: false)
        let resultJson = BridgeResponseFactory.buildReportAck(code: code, message: "Error report received", metadata: metadata)
        postResultToJs(resultJson, context: context, retryCount: 0, vendorCode: nil, retryable: nil, finalStatus: "success", resolvedByRetry: false)
    }

    private func setScenarioPreset(_ rawValue: String) {
        scenarioController.setActivePreset(rawValue)
        refreshDiagnosticsCache()
    }

    private func clearDiagnostics() {
        errorLogger.clear()
        refreshDiagnosticsCache()
    }

    private func executeChargeAttempt(cardId: String, amount: Int, context: RequestContext, attempt: Int) {
        let retryCount = max(0, attempt - 1)
        recordStage(context, stage: "sdk_start", retryCount: retryCount, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        adapter.requestCharge(cardId: cardId, amount: amount) { [weak self] result in
            self?.handleChargeResult(result, cardId: cardId, amount: amount, context: context, attempt: attempt)
        }
    }

    private func handleChargeResult(
        _ result: Result<ChargeResult, SdkErrorCode>,
        cardId: String,
        amount: Int,
        context: RequestContext,
        attempt: Int
    ) {
        switch result {
        case .success(let chargeResult):
            let retryCount = max(0, attempt - 1)
            let vendorCode = resolveSuccessVendorCode(context: context, retryCount: retryCount)
            let retryable = resolveSuccessRetryable(context: context, retryCount: retryCount)
            let decision = requestCoordinator.acceptSuccess(correlationId: context.correlationId, retryCount: retryCount)

            guard decision.accepted else {
                recordIgnoredSdkCallback(context: context, retryCount: retryCount, vendorCode: vendorCode, retryable: retryable, decision: decision)
                return
            }

            cancelTimeout(for: context.correlationId)
            let resolvedByRetry = retryCount > 0
            recordStage(context, stage: "sdk_callback", retryCount: retryCount, vendorCode: vendorCode, retryable: retryable, finalStatus: "success", resolvedByRetry: resolvedByRetry)

            let metadata = buildMetadata(context: context, stage: "js_callback", vendorCode: vendorCode, retryable: retryable, resolvedByRetry: resolvedByRetry)
            let resultJson = BridgeResponseFactory.buildChargeSuccess(chargeResult, retryCount: retryCount, metadata: metadata)
            postResultToJs(resultJson, context: context, retryCount: retryCount, vendorCode: vendorCode, retryable: retryable, finalStatus: "success", resolvedByRetry: resolvedByRetry)

        case .failure(let errorCode):
            handleRetryableFailure(
                method: "requestCharge",
                errorCode: errorCode,
                context: context,
                attempt: attempt,
                retryAction: { [weak self] nextAttempt in
                    self?.executeChargeAttempt(cardId: cardId, amount: amount, context: context, attempt: nextAttempt)
                }
            )
        }
    }

    private func executeBalanceAttempt(cardId: String, context: RequestContext, attempt: Int) {
        let retryCount = max(0, attempt - 1)
        recordStage(context, stage: "sdk_start", retryCount: retryCount, vendorCode: nil, retryable: nil, finalStatus: nil, resolvedByRetry: nil)
        adapter.getBalance(cardId: cardId) { [weak self] result in
            self?.handleBalanceResult(result, cardId: cardId, context: context, attempt: attempt)
        }
    }

    private func handleBalanceResult(
        _ result: Result<BalanceSnapshot, SdkErrorCode>,
        cardId: String,
        context: RequestContext,
        attempt: Int
    ) {
        switch result {
        case .success(let balanceResult):
            let retryCount = max(0, attempt - 1)
            let vendorCode = resolveSuccessVendorCode(context: context, retryCount: retryCount)
            let retryable = resolveSuccessRetryable(context: context, retryCount: retryCount)
            let decision = requestCoordinator.acceptSuccess(correlationId: context.correlationId, retryCount: retryCount)

            guard decision.accepted else {
                recordIgnoredSdkCallback(context: context, retryCount: retryCount, vendorCode: vendorCode, retryable: retryable, decision: decision)
                return
            }

            cancelTimeout(for: context.correlationId)
            let resolvedByRetry = retryCount > 0
            recordStage(context, stage: "sdk_callback", retryCount: retryCount, vendorCode: vendorCode, retryable: retryable, finalStatus: "success", resolvedByRetry: resolvedByRetry)

            let metadata = buildMetadata(context: context, stage: "js_callback", vendorCode: vendorCode, retryable: retryable, resolvedByRetry: resolvedByRetry)
            let resultJson = BridgeResponseFactory.buildBalanceSuccess(balanceResult, retryCount: retryCount, metadata: metadata)
            postResultToJs(resultJson, context: context, retryCount: retryCount, vendorCode: vendorCode, retryable: retryable, finalStatus: "success", resolvedByRetry: resolvedByRetry)

        case .failure(let errorCode):
            handleRetryableFailure(
                method: "getBalance",
                errorCode: errorCode,
                context: context,
                attempt: attempt,
                retryAction: { [weak self] nextAttempt in
                    self?.executeBalanceAttempt(cardId: cardId, context: context, attempt: nextAttempt)
                }
            )
        }
    }

    private func handleRetryableFailure(
        method: String,
        errorCode: SdkErrorCode,
        context: RequestContext,
        attempt: Int,
        retryAction: @escaping (Int) -> Void
    ) {
        let retryCount = max(0, attempt - 1)
        let finalAttempt = !errorCode.retryable || attempt > IOSNativeBridge.maxRetry

        if !finalAttempt {
            recordStage(
                context,
                stage: "sdk_callback",
                retryCount: retryCount,
                vendorCode: resolveVendorCode(errorCode: errorCode, scenario: context.scenario),
                retryable: errorCode.retryable,
                finalStatus: nil,
                resolvedByRetry: nil
            )

            let delay = IOSNativeBridge.backoffDelays[min(max(0, attempt - 1), IOSNativeBridge.backoffDelays.count - 1)]
            callbackQueue.asyncAfter(deadline: .now() + delay) {
                retryAction(attempt + 1)
            }
            return
        }

        let finalError: SdkErrorCode = errorCode.retryable && retryCount >= IOSNativeBridge.maxRetry ? .retryExhausted : errorCode
        let decision = requestCoordinator.acceptError(correlationId: context.correlationId, retryCount: retryCount)

        guard decision.accepted else {
            recordIgnoredSdkCallback(
                context: context,
                retryCount: retryCount,
                vendorCode: resolveVendorCode(errorCode: finalError, scenario: context.scenario),
                retryable: finalError.retryable,
                decision: decision
            )
            return
        }

        cancelTimeout(for: context.correlationId)
        let vendorCode = resolveVendorCode(errorCode: finalError, scenario: context.scenario)
        recordStage(context, stage: "sdk_callback", retryCount: retryCount, vendorCode: vendorCode, retryable: finalError.retryable, finalStatus: "error", resolvedByRetry: false)

        let metadata = buildMetadata(context: context, stage: "js_callback", vendorCode: vendorCode, retryable: finalError.retryable, resolvedByRetry: false)
        let resultJson = BridgeResponseFactory.buildError(
            method: method,
            errorCode: finalError,
            retryCount: retryCount,
            logId: UUID().uuidString,
            vendorCode: vendorCode,
            retryable: finalError.retryable,
            metadata: metadata
        )
        postResultToJs(resultJson, context: context, retryCount: retryCount, vendorCode: vendorCode, retryable: finalError.retryable, finalStatus: "error", resolvedByRetry: false)
    }

    private func beginTrackedRequest(_ context: RequestContext) {
        let registered = requestCoordinator.begin(
            correlationId: context.correlationId,
            method: context.method,
            callbackId: context.callbackId,
            scenario: context.scenario,
            startedAt: context.startedAt
        )
        scheduleTimeout(for: context, registered: registered)
        refreshDiagnosticsCache()
    }

    private func scheduleTimeout(for context: RequestContext, registered: BridgeRequestCoordinator.RegisteredRequest) {
        let delayMs = max(0, registered.timeoutAtMs - Timestamp.nowMs())
        let workItem = DispatchWorkItem { [weak self] in
            guard let self, !self.destroyed else {
                return
            }

            let decision = self.requestCoordinator.acceptTimeout(correlationId: context.correlationId)
            guard decision.accepted else {
                return
            }

            self.recordStage(
                context,
                stage: "timeout",
                retryCount: decision.retryCount,
                vendorCode: "VENDOR_TIMEOUT",
                retryable: false,
                finalStatus: "error",
                resolvedByRetry: false
            )

            let metadata = self.buildMetadata(context: context, stage: "timeout", vendorCode: "VENDOR_TIMEOUT", retryable: false, resolvedByRetry: false)
            let resultJson = BridgeResponseFactory.buildError(
                method: context.method,
                errorCode: .timeout,
                retryCount: decision.retryCount,
                logId: UUID().uuidString,
                vendorCode: "VENDOR_TIMEOUT",
                retryable: false,
                metadata: metadata
            )
            self.postResultToJs(resultJson, context: context, retryCount: decision.retryCount, vendorCode: "VENDOR_TIMEOUT", retryable: false, finalStatus: "error", resolvedByRetry: false)
        }

        timeoutWorkItems[context.correlationId] = workItem
        callbackQueue.asyncAfter(deadline: .now() + .milliseconds(Int(delayMs)), execute: workItem)
    }

    private func cancelTimeout(for correlationId: String) {
        timeoutWorkItems.removeValue(forKey: correlationId)?.cancel()
        refreshDiagnosticsCache()
    }

    private func postResultToJs(
        _ resultJson: String,
        context: RequestContext,
        retryCount: Int,
        vendorCode: String?,
        retryable: Bool?,
        finalStatus: String,
        resolvedByRetry: Bool
    ) {
        callbackQueue.async { [weak self] in
            guard let self else {
                return
            }

            if self.destroyed {
                self.recordStage(
                    context,
                    stage: "js_callback_ignored_destroyed",
                    retryCount: retryCount,
                    vendorCode: vendorCode,
                    retryable: retryable,
                    finalStatus: nil,
                    resolvedByRetry: nil
                )
                return
            }

            self.recordStage(
                context,
                stage: "js_callback",
                retryCount: retryCount,
                vendorCode: vendorCode,
                retryable: retryable,
                finalStatus: finalStatus,
                resolvedByRetry: resolvedByRetry
            )

            let script = "window.onBridgeResult(\(self.jsQuoted(resultJson)));"
            self.webView?.evaluateJavaScript(script, completionHandler: nil)
            self.refreshDiagnosticsCache()
        }
    }

    private func recordIgnoredSdkCallback(
        context: RequestContext,
        retryCount: Int,
        vendorCode: String?,
        retryable: Bool?,
        decision: BridgeRequestCoordinator.Decision
    ) {
        let stage: String
        switch decision.terminalState {
        case .success, .error:
            stage = "sdk_callback_ignored_duplicate"
        case .timedOut:
            stage = "sdk_callback_ignored_timeout"
        case .abandoned:
            stage = "sdk_callback_ignored_abandoned"
        case .pending:
            stage = "sdk_callback_ignored_pending"
        case .none:
            stage = "sdk_callback_ignored_missing"
        }

        recordStage(
            context,
            stage: stage,
            retryCount: retryCount,
            vendorCode: vendorCode,
            retryable: retryable,
            finalStatus: nil,
            resolvedByRetry: nil
        )
    }

    private func recordStage(
        _ context: RequestContext,
        stage: String,
        retryCount: Int,
        vendorCode: String?,
        retryable: Bool?,
        finalStatus: String?,
        resolvedByRetry: Bool?
    ) {
        var event = TimelineEvent.stage(
            correlationId: context.correlationId,
            method: context.method,
            scenario: context.scenario,
            stage: stage,
            timestamp: Timestamp.isoString(),
            retryCount: retryCount,
            durationMs: context.elapsedMs,
            vendorCode: vendorCode,
            retryable: retryable
        )

        if let finalStatus {
            event = event.withCompletion(finalStatus: finalStatus, resolvedByRetry: resolvedByRetry ?? false)
        }

        errorLogger.record(event)
        refreshDiagnosticsCache()
    }

    private func refreshDiagnosticsCache() {
        guard let webView else {
            return
        }

        let payload = DiagnosticsPayload(
            activePreset: scenarioController.activePreset.rawValue,
            availablePresets: ScenarioPreset.allValues,
            inFlightRequests: requestCoordinator.snapshot(),
            snapshot: errorLogger.getDiagnosticsSnapshot()
        )

        let payloadString = payload.jsonString()
        let quotedSnapshot = jsQuoted(payloadString)
        let script = """
        window.__railBridgeDiagnosticsSnapshot = \(quotedSnapshot);
        window.__railBridgeDiagnosticsExport = \(quotedSnapshot);
        """
        webView.evaluateJavaScript(script, completionHandler: nil)
    }

    private func runAutomationIfNeeded(on webView: WKWebView) {
        guard !didRunAutomation else {
            return
        }

        guard automation.preset != nil || automation.action != nil else {
            return
        }

        didRunAutomation = true
        let script = automation.script(jsQuoted: jsQuoted)

        callbackQueue.asyncAfter(deadline: .now() + 0.2) { [weak self, weak webView] in
            guard let self, let webView, !self.destroyed else {
                return
            }

            webView.evaluateJavaScript(script, completionHandler: nil)
        }
    }

    private func buildMetadata(
        context: RequestContext,
        stage: String,
        vendorCode: String?,
        retryable: Bool?,
        resolvedByRetry: Bool
    ) -> BridgeResponseFactory.Metadata {
        BridgeResponseFactory.Metadata(
            callbackId: context.callbackId,
            correlationId: context.correlationId,
            platform: IOSNativeBridge.platform,
            stage: stage,
            durationMs: context.elapsedMs,
            scenario: context.scenario,
            vendorCode: vendorCode,
            retryable: retryable,
            resolvedByRetry: resolvedByRetry ? true : nil
        )
    }

    private func resolveSuccessVendorCode(context: RequestContext, retryCount: Int) -> String? {
        guard retryCount > 0 else {
            return nil
        }

        let preset = ScenarioPreset.from(context.scenario)
        if preset == .timeout || preset == .retryExhausted {
            return "VENDOR_TIMEOUT"
        }
        return nil
    }

    private func resolveSuccessRetryable(context: RequestContext, retryCount: Int) -> Bool? {
        guard retryCount > 0 else {
            return nil
        }

        let preset = ScenarioPreset.from(context.scenario)
        if preset == .timeout || preset == .retryExhausted {
            return true
        }
        return nil
    }

    private func resolveVendorCode(errorCode: SdkErrorCode, scenario: String) -> String? {
        let preset = ScenarioPreset.from(scenario)
        if preset == .internalError || errorCode == .vendorInternal {
            return "VENDOR_INTERNAL"
        }
        if preset == .timeout || preset == .retryExhausted || errorCode == .networkTimeout || errorCode == .retryExhausted || errorCode == .timeout {
            return "VENDOR_TIMEOUT"
        }
        return nil
    }

    private func parseParams(_ raw: String) -> [String: Any] {
        guard
            let data = raw.data(using: .utf8),
            let value = try? JSONSerialization.jsonObject(with: data),
            let dictionary = value as? [String: Any]
        else {
            return [:]
        }

        return dictionary
    }

    private func jsQuoted(_ string: String) -> String {
        guard
            let data = try? JSONSerialization.data(withJSONObject: [string], options: []),
            let arrayString = String(data: data, encoding: .utf8)
        else {
            return "\"\""
        }

        return String(arrayString.dropFirst().dropLast())
    }

    private func injectedBridgeScript() -> String {
        let defaultPayload = DiagnosticsPayload(
            activePreset: ScenarioPreset.normal.rawValue,
            availablePresets: ScenarioPreset.allValues,
            inFlightRequests: [],
            snapshot: DiagnosticsSnapshot(exportedAt: "", timelines: [])
        ).jsonString()

        return """
        (function() {
          if (window.RailBridge) {
            return;
          }
          window.__railBridgeDiagnosticsSnapshot = \(jsQuoted(defaultPayload));
          window.__railBridgeDiagnosticsExport = window.__railBridgeDiagnosticsSnapshot;
          function post(method, payload) {
            if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.railBridge) {
              window.webkit.messageHandlers.railBridge.postMessage({ method: method, params: payload });
            }
          }
          window.RailBridge = {
            requestCharge: function(payload) { post("requestCharge", payload); },
            getBalance: function(payload) { post("getBalance", payload); },
            getSdkStatus: function() { post("getSdkStatus", ""); },
            reportError: function(payload) { post("reportError", payload); },
            setScenarioPreset: function(payload) { post("setScenarioPreset", payload); },
            getDiagnosticsSnapshot: function() { return window.__railBridgeDiagnosticsSnapshot || "{}"; },
            exportDiagnostics: function() { return window.__railBridgeDiagnosticsExport || window.__railBridgeDiagnosticsSnapshot || "{}"; },
            clearDiagnostics: function() { post("clearDiagnostics", ""); }
          };
          function notifyReady() {
            window.dispatchEvent(new Event("bridgeReady"));
          }
          if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", notifyReady, { once: true });
          } else {
            notifyReady();
          }
        })();
        """
    }

    private struct RequestContext {
        let method: String
        let callbackId: String?
        let correlationId: String
        let scenario: String
        let startedAt: String
        let startedAtMs: Int64

        init(method: String, callbackId: String?, scenario: String) {
            self.method = method
            self.callbackId = callbackId
            self.correlationId = UUID().uuidString
            self.scenario = ScenarioPreset.from(scenario).rawValue
            self.startedAt = Timestamp.isoString()
            self.startedAtMs = Timestamp.nowMs()
        }

        var elapsedMs: Int {
            max(0, Int(Timestamp.nowMs() - startedAtMs))
        }
    }
}

private struct AutomationConfig {
    let preset: String?
    let action: String?
    let cardId: String
    let amount: Int

    static var current: AutomationConfig {
        let environment = ProcessInfo.processInfo.environment
        return AutomationConfig(
            preset: environment["RAILBRIDGE_AUTOMATION_PRESET"]?.nilIfEmpty,
            action: environment["RAILBRIDGE_AUTOMATION_ACTION"]?.nilIfEmpty,
            cardId: environment["RAILBRIDGE_AUTOMATION_CARD_ID"]?.nilIfEmpty ?? "CARD_001",
            amount: Int(environment["RAILBRIDGE_AUTOMATION_AMOUNT"] ?? "") ?? 10000
        )
    }

    func script(jsQuoted: (String) -> String) -> String {
        var statements: [String] = []

        if let preset {
            statements.append("window.RailBridge.setScenarioPreset(\(jsQuoted(preset)));")
        }

        if let action {
            let actionStatement: String?
            switch action {
            case "requestCharge":
                actionStatement = "window.RailBridge.requestCharge(JSON.stringify({ cardId: \(jsQuoted(cardId)), amount: \(amount) }));"
            case "getBalance":
                actionStatement = "window.RailBridge.getBalance(JSON.stringify({ cardId: \(jsQuoted(cardId)) }));"
            case "getSdkStatus":
                actionStatement = "window.RailBridge.getSdkStatus();"
            case "reportError":
                actionStatement = """
                window.RailBridge.reportError(JSON.stringify({
                  code: "ERR_AUTOMATION_001",
                  message: "Synthetic JS error from automation",
                  context: "Codex runtime verification"
                }));
                """
            default:
                actionStatement = nil
            }

            if let actionStatement {
                if preset != nil {
                    statements.append("window.setTimeout(function() { \(actionStatement) }, 80);")
                } else {
                    statements.append(actionStatement)
                }
            }
        }

        let body = statements.joined(separator: "\n")
        return """
        (function() {
          if (!window.RailBridge) {
            return;
          }
          \(body)
        })();
        """
    }
}

private extension String {
    var nilIfEmpty: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
