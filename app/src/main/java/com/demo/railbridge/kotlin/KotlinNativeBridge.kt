package com.demo.railbridge.kotlin

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.demo.railbridge.bridge.InFlightRequestRecord
import com.demo.railbridge.logging.DiagnosticsSnapshot
import com.demo.railbridge.logging.ErrorLogger
import com.demo.railbridge.logging.RequestTimeline
import com.demo.railbridge.logging.TimelineEvent
import com.demo.railbridge.sdk.BalanceSnapshot
import com.demo.railbridge.sdk.ChargeResult
import com.demo.railbridge.sdk.RailPlusSdkAdapter
import com.demo.railbridge.sdk.ScenarioPreset
import com.demo.railbridge.sdk.SdkErrorCode
import com.demo.railbridge.sdk.SdkStatusSnapshot
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class KotlinNativeBridge(
    private val webView: WebView,
    private val sdkAdapter: RailPlusSdkAdapter,
    private val errorLogger: ErrorLogger
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestCoordinator = KotlinBridgeRequestCoordinator(REQUEST_TIMEOUT_MS)
    private val timeoutCallbacks = ConcurrentHashMap<String, Runnable>()

    @Volatile
    private var destroyed = false

    @JavascriptInterface
    fun requestCharge(paramsJson: String) {
        Log.d(TAG, "requestCharge called: $paramsJson")
        val requestContext = RequestContext.forMethod("requestCharge", paramsJson, resolveActivePreset())
        recordStage(requestContext, "js_entry", 0, null, null, null, null)

        try {
            val params = JSONObject(paramsJson)
            val cardId = params.optString("cardId", "")
            val amount = params.optInt("amount", 0)

            recordStage(requestContext, "native_validation", 0, null, null, null, null)
            beginTrackedRequest(
                requestContext,
                buildChargeFailureContext(cardId, amount, requestContext.correlationId)
            )
            executeChargeAttempt(cardId, amount, requestContext, 1)
        } catch (e: Exception) {
            Log.e(TAG, "requestCharge exception", e)
            errorLogger.logSdkFailure(
                "requestCharge",
                SdkErrorCode.ERR_UNKNOWN,
                "correlationId=${requestContext.correlationId}, params=$paramsJson, error=${e.message}"
            )
            recordStage(requestContext, "native_validation", 0, null, null, "error", false)
            val resultJson = KotlinBridgeResponseFactory.buildError(
                "requestCharge",
                SdkErrorCode.ERR_UNKNOWN,
                0,
                UUID.randomUUID().toString(),
                null,
                false,
                buildErrorMetadata(requestContext, 0, null, false)
            )
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false)
        }
    }

    @JavascriptInterface
    fun getBalance(paramsJson: String) {
        Log.d(TAG, "getBalance called: $paramsJson")
        val requestContext = RequestContext.forMethod("getBalance", paramsJson, resolveActivePreset())
        recordStage(requestContext, "js_entry", 0, null, null, null, null)

        try {
            val params = JSONObject(paramsJson)
            val cardId = params.optString("cardId", "")

            recordStage(requestContext, "native_validation", 0, null, null, null, null)
            beginTrackedRequest(
                requestContext,
                buildBalanceFailureContext(cardId, requestContext.correlationId)
            )
            executeBalanceAttempt(cardId, requestContext, 1)
        } catch (e: Exception) {
            Log.e(TAG, "getBalance exception", e)
            errorLogger.logSdkFailure(
                "getBalance",
                SdkErrorCode.ERR_UNKNOWN,
                "correlationId=${requestContext.correlationId}, params=$paramsJson, error=${e.message}"
            )
            recordStage(requestContext, "native_validation", 0, null, null, "error", false)
            val resultJson = KotlinBridgeResponseFactory.buildError(
                "getBalance",
                SdkErrorCode.ERR_UNKNOWN,
                0,
                UUID.randomUUID().toString(),
                null,
                false,
                buildErrorMetadata(requestContext, 0, null, false)
            )
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false)
        }
    }

    @JavascriptInterface
    fun getSdkStatus() {
        Log.d(TAG, "getSdkStatus called")
        val requestContext = RequestContext.forMethod("getSdkStatus", null, resolveActivePreset())
        recordStage(requestContext, "js_entry", 0, null, null, null, null)

        try {
            val status: SdkStatusSnapshot = sdkAdapter.status
            recordStage(requestContext, "native_validation", 0, null, null, null, null)
            val resultJson = KotlinBridgeResponseFactory.buildStatusSuccess(
                status,
                buildSuccessMetadata(requestContext, 0)
            )
            postResultToJs(resultJson, requestContext, 0, null, null, "success", false)
        } catch (e: Exception) {
            Log.e(TAG, "getSdkStatus exception", e)
            recordStage(requestContext, "native_validation", 0, null, null, "error", false)
            val resultJson = KotlinBridgeResponseFactory.buildError(
                "getSdkStatus",
                SdkErrorCode.ERR_UNKNOWN,
                0,
                UUID.randomUUID().toString(),
                null,
                false,
                buildErrorMetadata(requestContext, 0, null, false)
            )
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false)
        }
    }

    @JavascriptInterface
    fun reportError(errorJson: String) {
        Log.d(TAG, "reportError called: $errorJson")
        val requestContext = RequestContext.forMethod("reportError", errorJson, resolveActivePreset())
        recordStage(requestContext, "js_entry", 0, null, null, null, null)

        try {
            val error = JSONObject(errorJson)
            val code = error.optString("code", "UNKNOWN")
            val message = error.optString("message", "")
            val context = error.optString("context", "")
            recordStage(requestContext, "native_validation", 0, null, null, null, null)

            errorLogger.logSdkFailure(
                "reportError (from JS)",
                SdkErrorCode.ERR_UNKNOWN,
                "{\"code\":\"$code\",\"message\":\"$message\",\"context\":\"$context\",\"correlationId\":\"${requestContext.correlationId}\"}"
            )

            val resultJson = KotlinBridgeResponseFactory.buildReportAck(
                code,
                "Error report received",
                buildSuccessMetadata(requestContext, 0)
            )
            postResultToJs(resultJson, requestContext, 0, null, null, "success", false)
        } catch (e: Exception) {
            Log.e(TAG, "reportError exception", e)
            recordStage(requestContext, "native_validation", 0, null, null, "error", false)
            val resultJson = KotlinBridgeResponseFactory.buildError(
                "reportError",
                SdkErrorCode.ERR_UNKNOWN,
                0,
                UUID.randomUUID().toString(),
                null,
                false,
                buildErrorMetadata(requestContext, 0, null, false)
            )
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false)
        }
    }

    @JavascriptInterface
    fun setScenarioPreset(preset: String) {
        val adapter = sdkAdapter as? KotlinMockRailSdkAdapter ?: return
        adapter.getScenarioController().setActivePreset(preset)
    }

    @JavascriptInterface
    fun getDiagnosticsSnapshot(): String {
        return buildDiagnosticsPayload(
            resolveActivePreset(),
            errorLogger.diagnosticsSnapshot,
            requestCoordinator.snapshot()
        )
    }

    @JavascriptInterface
    fun exportDiagnostics(): String {
        return try {
            buildDiagnosticsPayload(
                resolveActivePreset(),
                DiagnosticsSnapshot.fromJsonObject(JSONObject(errorLogger.exportDiagnosticsJson())),
                requestCoordinator.snapshot()
            )
        } catch (_: JSONException) {
            buildDiagnosticsPayload(
                resolveActivePreset(),
                errorLogger.diagnosticsSnapshot,
                requestCoordinator.snapshot()
            )
        }
    }

    @JavascriptInterface
    fun clearDiagnostics() {
        errorLogger.clearLogs()
    }

    fun destroy() {
        destroyed = true
        timeoutCallbacks.values.forEach { mainHandler.removeCallbacks(it) }
        timeoutCallbacks.clear()
        for (abandoned in requestCoordinator.abandonAll()) {
            recordAbandonedRequest(abandoned)
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun executeChargeAttempt(cardId: String, amount: Int, requestContext: RequestContext, attempt: Int) {
        val retryCount = (attempt - 1).coerceAtLeast(0)
        recordStage(requestContext, "sdk_start", retryCount, null, null, null, null)
        sdkAdapter.requestCharge(cardId, amount, object : RailPlusSdkAdapter.Callback<ChargeResult> {
            override fun onSuccess(result: ChargeResult) {
                val vendorCode = resolveSuccessVendorCode(requestContext, retryCount)
                val retryable = resolveSuccessRetryable(requestContext, retryCount)
                val decision = requestCoordinator.acceptSuccess(requestContext.correlationId, retryCount)
                if (!decision.accepted) {
                    recordIgnoredSdkCallback(requestContext, retryCount, vendorCode, retryable, decision)
                    return
                }
                cancelTimeout(requestContext.correlationId)
                val resolvedByRetry = retryCount > 0
                recordStage(requestContext, "sdk_callback", retryCount, vendorCode, retryable, "success", resolvedByRetry)
                if (retryCount > 0) {
                    errorLogger.logRetrySuccess("requestCharge", retryCount)
                }
                val resultJson = KotlinBridgeResponseFactory.buildChargeSuccess(
                    result,
                    retryCount,
                    buildSuccessMetadata(requestContext, retryCount)
                )
                postResultToJs(resultJson, requestContext, retryCount, vendorCode, retryable, "success", resolvedByRetry)
            }

            override fun onError(errorCode: SdkErrorCode) {
                handleFailure(
                    method = "requestCharge",
                    requestContext = requestContext,
                    attempt = attempt,
                    errorCode = errorCode,
                    failureContext = buildChargeFailureContext(cardId, amount, requestContext.correlationId)
                ) {
                    executeChargeAttempt(cardId, amount, requestContext, attempt + 1)
                }
            }
        })
    }

    private fun executeBalanceAttempt(cardId: String, requestContext: RequestContext, attempt: Int) {
        val retryCount = (attempt - 1).coerceAtLeast(0)
        recordStage(requestContext, "sdk_start", retryCount, null, null, null, null)
        sdkAdapter.getBalance(cardId, object : RailPlusSdkAdapter.Callback<BalanceSnapshot> {
            override fun onSuccess(result: BalanceSnapshot) {
                val vendorCode = resolveSuccessVendorCode(requestContext, retryCount)
                val retryable = resolveSuccessRetryable(requestContext, retryCount)
                val decision = requestCoordinator.acceptSuccess(requestContext.correlationId, retryCount)
                if (!decision.accepted) {
                    recordIgnoredSdkCallback(requestContext, retryCount, vendorCode, retryable, decision)
                    return
                }
                cancelTimeout(requestContext.correlationId)
                val resolvedByRetry = retryCount > 0
                recordStage(requestContext, "sdk_callback", retryCount, vendorCode, retryable, "success", resolvedByRetry)
                if (retryCount > 0) {
                    errorLogger.logRetrySuccess("getBalance", retryCount)
                }
                val resultJson = KotlinBridgeResponseFactory.buildBalanceSuccess(
                    result,
                    retryCount,
                    buildSuccessMetadata(requestContext, retryCount)
                )
                postResultToJs(resultJson, requestContext, retryCount, vendorCode, retryable, "success", resolvedByRetry)
            }

            override fun onError(errorCode: SdkErrorCode) {
                handleFailure(
                    method = "getBalance",
                    requestContext = requestContext,
                    attempt = attempt,
                    errorCode = errorCode,
                    failureContext = buildBalanceFailureContext(cardId, requestContext.correlationId)
                ) {
                    executeBalanceAttempt(cardId, requestContext, attempt + 1)
                }
            }
        })
    }

    private fun handleFailure(
        method: String,
        requestContext: RequestContext,
        attempt: Int,
        errorCode: SdkErrorCode,
        failureContext: String,
        retryAction: () -> Unit
    ) {
        val retryCount = (attempt - 1).coerceAtLeast(0)
        errorLogger.logRetryAttempt(method, attempt, errorCode)

        if (isRetryable(errorCode) && attempt <= MAX_RETRY) {
            recordStage(
                requestContext,
                "sdk_callback",
                retryCount,
                resolveVendorCode(errorCode, requestContext.scenario),
                true,
                null,
                null
            )
            mainHandler.postDelayed(retryAction, getBackoffDelay(attempt))
            return
        }

        val boundedRetryCount = retryCount.coerceAtMost(MAX_RETRY)
        val finalError = if (isRetryable(errorCode) && boundedRetryCount >= MAX_RETRY) {
            SdkErrorCode.RETRY_EXHAUSTED
        } else {
            errorCode
        }
        val vendorCode = resolveVendorCode(finalError, requestContext.scenario)
        val retryable = isRetryable(finalError)
        val decision = requestCoordinator.acceptError(requestContext.correlationId, boundedRetryCount, finalError)
        if (!decision.accepted) {
            recordIgnoredSdkCallback(requestContext, boundedRetryCount, vendorCode, retryable, decision)
            return
        }
        cancelTimeout(requestContext.correlationId)
        errorLogger.logSdkFailure(method, finalError, failureContext)
        recordStage(requestContext, "sdk_callback", boundedRetryCount, vendorCode, retryable, "error", false)
        val resultJson = KotlinBridgeResponseFactory.buildError(
            method,
            finalError,
            boundedRetryCount,
            UUID.randomUUID().toString(),
            vendorCode,
            retryable,
            buildErrorMetadata(requestContext, boundedRetryCount, vendorCode, retryable)
        )
        postResultToJs(resultJson, requestContext, boundedRetryCount, vendorCode, retryable, "error", false)
    }

    private fun beginTrackedRequest(requestContext: RequestContext, failureContext: String) {
        val registeredRequest = requestCoordinator.begin(
            requestContext.correlationId,
            requestContext.method,
            requestContext.callbackId,
            requestContext.scenario,
            requestContext.startedAt
        )
        scheduleTimeout(requestContext, registeredRequest, failureContext)
    }

    private fun scheduleTimeout(
        requestContext: RequestContext,
        registeredRequest: KotlinBridgeRequestCoordinator.RegisteredRequest,
        failureContext: String
    ) {
        val delayMs = (registeredRequest.timeoutAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val runnable = Runnable {
            if (destroyed) return@Runnable
            val decision = requestCoordinator.acceptTimeout(requestContext.correlationId)
            if (!decision.accepted) return@Runnable
            errorLogger.logSdkFailure(requestContext.method, SdkErrorCode.ERR_TIMEOUT, failureContext)
            recordStage(requestContext, "timeout", decision.retryCount, VENDOR_TIMEOUT, false, "error", false)
            val resultJson = KotlinBridgeResponseFactory.buildError(
                requestContext.method,
                SdkErrorCode.ERR_TIMEOUT,
                decision.retryCount,
                UUID.randomUUID().toString(),
                VENDOR_TIMEOUT,
                false,
                buildTimeoutMetadata(requestContext, decision.retryCount)
            )
            postResultToJs(resultJson, requestContext, decision.retryCount, VENDOR_TIMEOUT, false, "error", false)
        }
        timeoutCallbacks[requestContext.correlationId] = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelTimeout(correlationId: String) {
        timeoutCallbacks.remove(correlationId)?.let { mainHandler.removeCallbacks(it) }
    }

    private fun postResultToJs(
        resultJson: String,
        requestContext: RequestContext,
        retryCount: Int,
        vendorCode: String?,
        retryable: Boolean?,
        finalStatus: String,
        resolvedByRetry: Boolean
    ) {
        mainHandler.post {
            if (destroyed) {
                recordStage(requestContext, "js_callback_ignored_destroyed", retryCount, vendorCode, retryable, null, null)
                return@post
            }
            recordStage(requestContext, "js_callback", retryCount, vendorCode, retryable, finalStatus, resolvedByRetry)
            val jsCode = "window.onBridgeResult(${JSONObject.quote(resultJson)})"
            webView.evaluateJavascript(jsCode, null)
        }
    }

    private fun recordIgnoredSdkCallback(
        requestContext: RequestContext,
        retryCount: Int,
        vendorCode: String?,
        retryable: Boolean?,
        decision: KotlinBridgeRequestCoordinator.Decision
    ) {
        recordStage(requestContext, resolveIgnoredCallbackStage(decision), retryCount, vendorCode, retryable, null, null)
    }

    private fun resolveIgnoredCallbackStage(decision: KotlinBridgeRequestCoordinator.Decision): String {
        return when (decision.terminalState) {
            KotlinBridgeRequestCoordinator.TerminalState.SUCCESS,
            KotlinBridgeRequestCoordinator.TerminalState.ERROR -> "sdk_callback_ignored_duplicate"
            KotlinBridgeRequestCoordinator.TerminalState.TIMED_OUT -> "sdk_callback_ignored_timeout"
            KotlinBridgeRequestCoordinator.TerminalState.ABANDONED -> "sdk_callback_ignored_abandoned"
            KotlinBridgeRequestCoordinator.TerminalState.PENDING -> "sdk_callback_ignored_pending"
            null -> "sdk_callback_ignored_missing"
        }
    }

    private fun recordAbandonedRequest(abandoned: InFlightRequestRecord) {
        errorLogger.recordEvent(
            TimelineEvent.stage(
                abandoned.correlationId,
                abandoned.method,
                abandoned.scenario,
                "bridge_abandoned",
                Instant.now().toString(),
                0,
                abandoned.elapsedMs,
                null,
                null
            )
        )
    }

    private fun buildSuccessMetadata(requestContext: RequestContext, retryCount: Int): KotlinBridgeResponseFactory.Metadata {
        return buildMetadata(
            requestContext,
            "js_callback",
            resolveSuccessVendorCode(requestContext, retryCount),
            resolveSuccessRetryable(requestContext, retryCount),
            retryCount > 0
        )
    }

    private fun buildErrorMetadata(
        requestContext: RequestContext,
        retryCount: Int,
        vendorCode: String?,
        retryable: Boolean?
    ): KotlinBridgeResponseFactory.Metadata {
        return buildMetadata(
            requestContext,
            "js_callback",
            vendorCode,
            retryable,
            retryCount > 0 && retryable == true
        )
    }

    private fun buildTimeoutMetadata(requestContext: RequestContext, retryCount: Int): KotlinBridgeResponseFactory.Metadata {
        return buildMetadata(requestContext, "timeout", VENDOR_TIMEOUT, false, retryCount > 0)
    }

    private fun buildMetadata(
        requestContext: RequestContext,
        stage: String,
        vendorCode: String?,
        retryable: Boolean?,
        resolvedByRetry: Boolean?
    ): KotlinBridgeResponseFactory.Metadata {
        return KotlinBridgeResponseFactory.Metadata(
            callbackId = requestContext.callbackId,
            correlationId = requestContext.correlationId,
            platform = PLATFORM_ANDROID,
            stage = stage,
            durationMs = requestContext.elapsedMs(),
            scenario = requestContext.scenario,
            vendorCode = vendorCode,
            retryable = retryable,
            resolvedByRetry = resolvedByRetry
        )
    }

    private fun recordStage(
        requestContext: RequestContext,
        stage: String,
        retryCount: Int,
        vendorCode: String?,
        retryable: Boolean?,
        finalStatus: String?,
        resolvedByRetry: Boolean?
    ) {
        var event = TimelineEvent.stage(
            requestContext.correlationId,
            requestContext.method,
            requestContext.scenario,
            stage,
            Instant.now().toString(),
            retryCount,
            requestContext.elapsedMs(),
            vendorCode,
            retryable
        )
        if (finalStatus != null) {
            event = event.withCompletion(finalStatus, resolvedByRetry == true)
        }
        errorLogger.recordEvent(event)
    }

    private fun buildChargeFailureContext(cardId: String, amount: Int, correlationId: String): String {
        return "{\"cardId\":\"$cardId\",\"amount\":$amount,\"correlationId\":\"$correlationId\"}"
    }

    private fun buildBalanceFailureContext(cardId: String, correlationId: String): String {
        return "{\"cardId\":\"$cardId\",\"correlationId\":\"$correlationId\"}"
    }

    private fun resolveActivePreset(): String {
        val adapter = sdkAdapter as? KotlinMockRailSdkAdapter ?: return ScenarioPreset.NORMAL.value
        return adapter.getScenarioController().activePreset.value
    }

    private fun resolveSuccessVendorCode(requestContext: RequestContext, retryCount: Int): String? {
        if (retryCount <= 0) return null
        val preset = ScenarioPreset.fromValue(requestContext.scenario)
        return if (preset == ScenarioPreset.TIMEOUT || preset == ScenarioPreset.RETRY_EXHAUSTED) {
            VENDOR_TIMEOUT
        } else {
            null
        }
    }

    private fun resolveSuccessRetryable(requestContext: RequestContext, retryCount: Int): Boolean? {
        if (retryCount <= 0) return null
        val preset = ScenarioPreset.fromValue(requestContext.scenario)
        return if (preset == ScenarioPreset.TIMEOUT || preset == ScenarioPreset.RETRY_EXHAUSTED) true else null
    }

    private fun resolveVendorCode(errorCode: SdkErrorCode, scenario: String): String? {
        val preset = ScenarioPreset.fromValue(scenario)
        return when {
            preset == ScenarioPreset.INTERNAL_ERROR || errorCode == SdkErrorCode.ERR_VENDOR_INTERNAL -> VENDOR_INTERNAL
            preset == ScenarioPreset.TIMEOUT ||
                preset == ScenarioPreset.RETRY_EXHAUSTED ||
                errorCode == SdkErrorCode.ERR_NETWORK_TIMEOUT ||
                errorCode == SdkErrorCode.RETRY_EXHAUSTED ||
                errorCode == SdkErrorCode.ERR_TIMEOUT -> VENDOR_TIMEOUT
            else -> null
        }
    }

    private fun isRetryable(errorCode: SdkErrorCode): Boolean {
        return errorCode == SdkErrorCode.ERR_NETWORK_TIMEOUT || errorCode == SdkErrorCode.ERR_SDK_INTERNAL
    }

    private fun getBackoffDelay(retryNumber: Int): Long {
        val index = (retryNumber - 1).coerceIn(0, BACKOFF_DELAYS.lastIndex)
        return BACKOFF_DELAYS[index]
    }

    companion object {
        private const val TAG = "RailBridge.KotlinNativeBridge"
        private const val PLATFORM_ANDROID = "android"
        private const val REQUEST_TIMEOUT_MS = 5000L
        private const val VENDOR_TIMEOUT = "VENDOR_TIMEOUT"
        private const val VENDOR_INTERNAL = "VENDOR_INTERNAL"
        private const val MAX_RETRY = 3
        private val BACKOFF_DELAYS = longArrayOf(500L, 1000L, 2000L)

        @JvmStatic
        fun buildDiagnosticsPayload(activePreset: String, snapshot: DiagnosticsSnapshot): String {
            return buildDiagnosticsPayload(activePreset, snapshot, emptyList())
        }

        @JvmStatic
        fun buildDiagnosticsPayload(
            activePreset: String,
            snapshot: DiagnosticsSnapshot,
            inFlightRequests: List<InFlightRequestRecord>
        ): String {
            return try {
                val payload = JSONObject()
                payload.put("activePreset", ScenarioPreset.fromValue(activePreset).value)

                val presets = JSONArray()
                for (preset in ScenarioPreset.values()) {
                    presets.put(preset.value)
                }
                payload.put("availablePresets", presets)

                val inFlightArray = JSONArray()
                inFlightRequests.forEach { inFlightArray.put(it.toJsonObject()) }
                payload.put("inFlightRequests", inFlightArray)

                payload.put(
                    "snapshot",
                    snapshot.toJsonObject()
                )
                payload.toString()
            } catch (_: JSONException) {
                "{\"activePreset\":\"${ScenarioPreset.fromValue(activePreset).value}\"}"
            }
        }
    }

    private data class RequestContext(
        val method: String,
        val callbackId: String?,
        val correlationId: String,
        val scenario: String,
        val startedAt: String,
        val startedAtMs: Long
    ) {
        fun elapsedMs(): Long = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)

        companion object {
            fun forMethod(method: String, paramsJson: String?, scenario: String): RequestContext {
                var callbackId: String? = null
                if (!paramsJson.isNullOrBlank()) {
                    try {
                        val payload = JSONObject(paramsJson)
                        callbackId = if (payload.has("callbackId") && !payload.isNull("callbackId")) {
                            payload.optString("callbackId")
                        } else {
                            null
                        }
                    } catch (_: JSONException) {
                        callbackId = null
                    }
                }
                val startedAtMs = System.currentTimeMillis()
                return RequestContext(
                    method = method,
                    callbackId = callbackId,
                    correlationId = UUID.randomUUID().toString(),
                    scenario = ScenarioPreset.fromValue(scenario).value,
                    startedAt = Instant.now().toString(),
                    startedAtMs = startedAtMs
                )
            }
        }
    }
}
