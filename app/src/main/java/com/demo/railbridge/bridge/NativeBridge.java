package com.demo.railbridge.bridge;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.demo.railbridge.logging.ErrorLogger;
import com.demo.railbridge.logging.DiagnosticsSnapshot;
import com.demo.railbridge.logging.RequestTimeline;
import com.demo.railbridge.logging.TimelineEvent;
import com.demo.railbridge.retry.RetryHandler;
import com.demo.railbridge.sdk.BalanceSnapshot;
import com.demo.railbridge.sdk.ChargeResult;
import com.demo.railbridge.sdk.MockRailSdkAdapter;
import com.demo.railbridge.sdk.RailPlusSdkAdapter;
import com.demo.railbridge.sdk.ScenarioPreset;
import com.demo.railbridge.sdk.SdkErrorCode;
import com.demo.railbridge.sdk.SdkStatusSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class NativeBridge {

    private static final String TAG = "RailBridge.NativeBridge";
    private static final String PLATFORM_ANDROID = "android";

    private final WebView webView;
    private final RailPlusSdkAdapter sdkAdapter;
    private final ErrorLogger errorLogger;
    private final RetryHandler retryHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean destroyed;

    public NativeBridge(WebView webView, RailPlusSdkAdapter sdkAdapter, ErrorLogger errorLogger) {
        this.webView = webView;
        this.sdkAdapter = sdkAdapter;
        this.errorLogger = errorLogger;
        this.retryHandler = new RetryHandler(errorLogger);
    }

    @JavascriptInterface
    public void requestCharge(String paramsJson) {
        Log.d(TAG, "requestCharge called: " + paramsJson);
        RequestContext requestContext = RequestContext.forMethod(
                "requestCharge",
                paramsJson,
                resolveActivePreset()
        );
        recordStage(requestContext, "js_entry", 0, null, null, null, null);

        try {
            JSONObject params = new JSONObject(paramsJson);
            final String cardId = params.optString("cardId", "");
            final int amount = params.optInt("amount", 0);
            final AtomicInteger sdkAttempts = new AtomicInteger(0);

            recordStage(requestContext, "native_validation", 0, null, null, null, null);

            retryHandler.execute(new RetryHandler.RetryTask() {
                @Override
                public void run(
                        RetryHandler.OnSuccessCallback onSuccess,
                        RetryHandler.OnErrorCallback onError
                ) {
                    final int attempt = sdkAttempts.incrementAndGet();
                    recordStage(requestContext, "sdk_start", Math.max(0, attempt - 1), null, null, null, null);
                    sdkAdapter.requestCharge(cardId, amount, new RailPlusSdkAdapter.Callback<ChargeResult>() {
                        @Override
                        public void onSuccess(ChargeResult result) {
                            int retryCount = Math.max(0, attempt - 1);
                            String vendorCode = resolveSuccessVendorCode(requestContext, retryCount);
                            Boolean retryable = resolveSuccessRetryable(requestContext, retryCount);
                            boolean resolvedByRetry = retryCount > 0;
                            recordStage(
                                    requestContext,
                                    "sdk_callback",
                                    retryCount,
                                    vendorCode,
                                    retryable,
                                    "success",
                                    resolvedByRetry
                            );
                            onSuccess.onResult(BridgeResponseFactory.buildChargeSuccess(result, null));
                        }

                        @Override
                        public void onError(SdkErrorCode errorCode) {
                            boolean finalAttempt = isFinalAttempt(errorCode, attempt);
                            recordStage(
                                    requestContext,
                                    "sdk_callback",
                                    Math.max(0, attempt - 1),
                                    resolveVendorCode(errorCode, requestContext.getScenario()),
                                    RetryHandler.isRetryable(errorCode),
                                    finalAttempt ? "error" : null,
                                    finalAttempt ? Boolean.FALSE : null
                            );
                            onError.onError(errorCode);
                        }
                    });
                }

                @Override
                public String getMethodName() {
                    return "requestCharge";
                }
            }, new RetryHandler.RetryCallback() {
                @Override
                public void onSuccess(String resultJson) {
                    String resultWithMetadata = BridgeResponseFactory.attachMetadata(
                            resultJson,
                            buildSuccessMetadata(requestContext, extractRetryCount(resultJson))
                    );
                    Log.d(TAG, "requestCharge success: " + resultWithMetadata);
                    postResultToJs(
                            resultWithMetadata,
                            requestContext,
                            extractRetryCount(resultWithMetadata),
                            resolveSuccessVendorCode(requestContext, extractRetryCount(resultWithMetadata)),
                            resolveSuccessRetryable(requestContext, extractRetryCount(resultWithMetadata)),
                            "success",
                            extractRetryCount(resultWithMetadata) > 0
                    );
                }

                @Override
                public void onError(SdkErrorCode errorCode, int retryCount) {
                    Log.e(TAG, "requestCharge failed: " + errorCode);
                    errorLogger.logSdkFailure(
                            "requestCharge",
                            errorCode,
                            buildFailureContext(cardId, amount, requestContext.getCorrelationId())
                    );
                    String vendorCode = resolveVendorCode(errorCode, requestContext.getScenario());
                    String resultJson = BridgeResponseFactory.buildError(
                            "requestCharge",
                            errorCode,
                            retryCount,
                            UUID.randomUUID().toString(),
                            vendorCode,
                            RetryHandler.isRetryable(errorCode),
                            buildErrorMetadata(requestContext, retryCount, vendorCode, RetryHandler.isRetryable(errorCode))
                    );
                    postResultToJs(
                            resultJson,
                            requestContext,
                            retryCount,
                            vendorCode,
                            RetryHandler.isRetryable(errorCode),
                            "error",
                            false
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "requestCharge exception", e);
            errorLogger.logSdkFailure(
                    "requestCharge",
                    SdkErrorCode.ERR_UNKNOWN,
                    "correlationId=" + requestContext.getCorrelationId()
                            + ", params=" + paramsJson
                            + ", error=" + e.getMessage()
            );
            recordStage(requestContext, "native_validation", 0, null, null, "error", Boolean.FALSE);
            String resultJson = BridgeResponseFactory.buildError(
                    "requestCharge",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildErrorMetadata(requestContext, 0, null, false)
            );
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false);
        }
    }

    @JavascriptInterface
    public void getBalance(String paramsJson) {
        Log.d(TAG, "getBalance called: " + paramsJson);
        RequestContext requestContext = RequestContext.forMethod(
                "getBalance",
                paramsJson,
                resolveActivePreset()
        );
        recordStage(requestContext, "js_entry", 0, null, null, null, null);

        try {
            JSONObject params = new JSONObject(paramsJson);
            final String cardId = params.optString("cardId", "");
            final AtomicInteger sdkAttempts = new AtomicInteger(0);

            recordStage(requestContext, "native_validation", 0, null, null, null, null);

            retryHandler.execute(new RetryHandler.RetryTask() {
                @Override
                public void run(
                        RetryHandler.OnSuccessCallback onSuccess,
                        RetryHandler.OnErrorCallback onError
                ) {
                    final int attempt = sdkAttempts.incrementAndGet();
                    recordStage(requestContext, "sdk_start", Math.max(0, attempt - 1), null, null, null, null);
                    sdkAdapter.getBalance(cardId, new RailPlusSdkAdapter.Callback<BalanceSnapshot>() {
                        @Override
                        public void onSuccess(BalanceSnapshot result) {
                            int retryCount = Math.max(0, attempt - 1);
                            String vendorCode = resolveSuccessVendorCode(requestContext, retryCount);
                            Boolean retryable = resolveSuccessRetryable(requestContext, retryCount);
                            boolean resolvedByRetry = retryCount > 0;
                            recordStage(
                                    requestContext,
                                    "sdk_callback",
                                    retryCount,
                                    vendorCode,
                                    retryable,
                                    "success",
                                    resolvedByRetry
                            );
                            onSuccess.onResult(BridgeResponseFactory.buildBalanceSuccess(result, null));
                        }

                        @Override
                        public void onError(SdkErrorCode errorCode) {
                            boolean finalAttempt = isFinalAttempt(errorCode, attempt);
                            recordStage(
                                    requestContext,
                                    "sdk_callback",
                                    Math.max(0, attempt - 1),
                                    resolveVendorCode(errorCode, requestContext.getScenario()),
                                    RetryHandler.isRetryable(errorCode),
                                    finalAttempt ? "error" : null,
                                    finalAttempt ? Boolean.FALSE : null
                            );
                            onError.onError(errorCode);
                        }
                    });
                }

                @Override
                public String getMethodName() {
                    return "getBalance";
                }
            }, new RetryHandler.RetryCallback() {
                @Override
                public void onSuccess(String resultJson) {
                    String resultWithMetadata = BridgeResponseFactory.attachMetadata(
                            resultJson,
                            buildSuccessMetadata(requestContext, extractRetryCount(resultJson))
                    );
                    Log.d(TAG, "getBalance success: " + resultWithMetadata);
                    postResultToJs(
                            resultWithMetadata,
                            requestContext,
                            extractRetryCount(resultWithMetadata),
                            resolveSuccessVendorCode(requestContext, extractRetryCount(resultWithMetadata)),
                            resolveSuccessRetryable(requestContext, extractRetryCount(resultWithMetadata)),
                            "success",
                            extractRetryCount(resultWithMetadata) > 0
                    );
                }

                @Override
                public void onError(SdkErrorCode errorCode, int retryCount) {
                    Log.e(TAG, "getBalance failed: " + errorCode);
                    errorLogger.logSdkFailure(
                            "getBalance",
                            errorCode,
                            "{\"cardId\":\"" + cardId + "\",\"correlationId\":\""
                                    + requestContext.getCorrelationId() + "\"}"
                    );
                    String vendorCode = resolveVendorCode(errorCode, requestContext.getScenario());
                    String resultJson = BridgeResponseFactory.buildError(
                            "getBalance",
                            errorCode,
                            retryCount,
                            UUID.randomUUID().toString(),
                            vendorCode,
                            RetryHandler.isRetryable(errorCode),
                            buildErrorMetadata(requestContext, retryCount, vendorCode, RetryHandler.isRetryable(errorCode))
                    );
                    postResultToJs(
                            resultJson,
                            requestContext,
                            retryCount,
                            vendorCode,
                            RetryHandler.isRetryable(errorCode),
                            "error",
                            false
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getBalance exception", e);
            errorLogger.logSdkFailure(
                    "getBalance",
                    SdkErrorCode.ERR_UNKNOWN,
                    "correlationId=" + requestContext.getCorrelationId()
                            + ", params=" + paramsJson
                            + ", error=" + e.getMessage()
            );
            recordStage(requestContext, "native_validation", 0, null, null, "error", Boolean.FALSE);
            String resultJson = BridgeResponseFactory.buildError(
                    "getBalance",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildErrorMetadata(requestContext, 0, null, false)
            );
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false);
        }
    }

    @JavascriptInterface
    public void getSdkStatus() {
        Log.d(TAG, "getSdkStatus called");
        RequestContext requestContext = RequestContext.forMethod(
                "getSdkStatus",
                null,
                resolveActivePreset()
        );
        recordStage(requestContext, "js_entry", 0, null, null, null, null);

        try {
            SdkStatusSnapshot status = sdkAdapter.getStatus();
            recordStage(requestContext, "native_validation", 0, null, null, null, null);
            String resultJson = BridgeResponseFactory.buildStatusSuccess(
                    status,
                    buildSuccessMetadata(requestContext, 0)
            );
            postResultToJs(resultJson, requestContext, 0, null, null, "success", false);
        } catch (Exception e) {
            Log.e(TAG, "getSdkStatus exception", e);
            recordStage(requestContext, "native_validation", 0, null, null, "error", Boolean.FALSE);
            String resultJson = BridgeResponseFactory.buildError(
                    "getSdkStatus",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildErrorMetadata(requestContext, 0, null, false)
            );
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false);
        }
    }

    @JavascriptInterface
    public void reportError(String errorJson) {
        Log.d(TAG, "reportError called: " + errorJson);
        RequestContext requestContext = RequestContext.forMethod(
                "reportError",
                errorJson,
                resolveActivePreset()
        );
        recordStage(requestContext, "js_entry", 0, null, null, null, null);

        try {
            JSONObject error = new JSONObject(errorJson);
            String code = error.optString("code", "UNKNOWN");
            String message = error.optString("message", "");
            String context = error.optString("context", "");
            recordStage(requestContext, "native_validation", 0, null, null, null, null);

            errorLogger.logSdkFailure(
                    "reportError (from JS)",
                    SdkErrorCode.ERR_UNKNOWN,
                    "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"context\":\"" + context
                            + "\",\"correlationId\":\"" + requestContext.getCorrelationId() + "\"}"
            );

            String resultJson = BridgeResponseFactory.buildReportAck(
                    code,
                    "Error report received",
                    buildSuccessMetadata(requestContext, 0)
            );
            postResultToJs(resultJson, requestContext, 0, null, null, "success", false);
        } catch (Exception e) {
            Log.e(TAG, "reportError exception", e);
            recordStage(requestContext, "native_validation", 0, null, null, "error", Boolean.FALSE);
            String resultJson = BridgeResponseFactory.buildError(
                    "reportError",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildErrorMetadata(requestContext, 0, null, false)
            );
            postResultToJs(resultJson, requestContext, 0, null, false, "error", false);
        }
    }

    @JavascriptInterface
    public void setScenarioPreset(String preset) {
        MockRailSdkAdapter adapter = getScenarioCapableAdapter();
        if (adapter != null) {
            adapter.getScenarioController().setActivePreset(preset);
        }
    }

    @JavascriptInterface
    public String getDiagnosticsSnapshot() {
        return buildDiagnosticsPayload(resolveActivePreset(), errorLogger.getDiagnosticsSnapshot());
    }

    @JavascriptInterface
    public String exportDiagnostics() {
        try {
            return buildDiagnosticsPayload(
                    resolveActivePreset(),
                    DiagnosticsSnapshot.fromJsonObject(new JSONObject(errorLogger.exportDiagnosticsJson()))
            );
        } catch (JSONException e) {
            return buildDiagnosticsPayload(resolveActivePreset(), errorLogger.getDiagnosticsSnapshot());
        }
    }

    @JavascriptInterface
    public void clearDiagnostics() {
        errorLogger.clearLogs();
    }

    public void destroy() {
        destroyed = true;
        retryHandler.cancel();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void postResultToJs(
            String resultJson,
            RequestContext requestContext,
            int retryCount,
            String vendorCode,
            Boolean retryable,
            String finalStatus,
            boolean resolvedByRetry
    ) {
        mainHandler.post(() -> {
            if (destroyed) {
                return;
            }
            recordStage(
                    requestContext,
                    "js_callback",
                    retryCount,
                    vendorCode,
                    retryable,
                    finalStatus,
                    resolvedByRetry
            );
            String jsCode = "window.onBridgeResult(" + JSONObject.quote(resultJson) + ")";
            webView.evaluateJavascript(jsCode, null);
        });
    }

    private BridgeResponseFactory.Metadata buildSuccessMetadata(RequestContext requestContext, int retryCount) {
        return buildMetadata(
                requestContext,
                "js_callback",
                resolveSuccessVendorCode(requestContext, retryCount),
                resolveSuccessRetryable(requestContext, retryCount),
                retryCount > 0
        );
    }

    private BridgeResponseFactory.Metadata buildErrorMetadata(
            RequestContext requestContext,
            int retryCount,
            String vendorCode,
            Boolean retryable
    ) {
        return buildMetadata(
                requestContext,
                "js_callback",
                vendorCode,
                retryable,
                retryCount > 0 && Boolean.TRUE.equals(retryable)
        );
    }

    private BridgeResponseFactory.Metadata buildMetadata(
            RequestContext requestContext,
            String stage,
            String vendorCode,
            Boolean retryable,
            Boolean resolvedByRetry
    ) {
        return new BridgeResponseFactory.Metadata(
                requestContext.getCallbackId(),
                requestContext.getCorrelationId(),
                PLATFORM_ANDROID,
                stage,
                requestContext.elapsedMs(),
                requestContext.getScenario(),
                vendorCode,
                retryable,
                resolvedByRetry
        );
    }

    private void recordStage(
            RequestContext requestContext,
            String stage,
            int retryCount,
            String vendorCode,
            Boolean retryable,
            String finalStatus,
            Boolean resolvedByRetry
    ) {
        TimelineEvent event = TimelineEvent.stage(
                requestContext.getCorrelationId(),
                requestContext.getMethod(),
                requestContext.getScenario(),
                stage,
                Instant.now().toString(),
                retryCount,
                requestContext.elapsedMs(),
                vendorCode,
                retryable
        );
        if (finalStatus != null) {
            event = event.withCompletion(finalStatus, Boolean.TRUE.equals(resolvedByRetry));
        }
        errorLogger.recordEvent(event);
    }

    private String buildFailureContext(String cardId, int amount, String correlationId) {
        return "{\"cardId\":\"" + cardId + "\",\"amount\":" + amount
                + ",\"correlationId\":\"" + correlationId + "\"}";
    }

    private MockRailSdkAdapter getScenarioCapableAdapter() {
        if (sdkAdapter instanceof MockRailSdkAdapter) {
            return (MockRailSdkAdapter) sdkAdapter;
        }
        return null;
    }

    private String resolveActivePreset() {
        MockRailSdkAdapter adapter = getScenarioCapableAdapter();
        if (adapter == null) {
            return ScenarioPreset.NORMAL.getValue();
        }
        return adapter.getScenarioController().getActivePreset().getValue();
    }

    private String resolveSuccessVendorCode(RequestContext requestContext, int retryCount) {
        if (retryCount <= 0) {
            return null;
        }
        ScenarioPreset preset = ScenarioPreset.fromValue(requestContext.getScenario());
        if (preset == ScenarioPreset.TIMEOUT || preset == ScenarioPreset.RETRY_EXHAUSTED) {
            return "VENDOR_TIMEOUT";
        }
        return null;
    }

    private Boolean resolveSuccessRetryable(RequestContext requestContext, int retryCount) {
        if (retryCount <= 0) {
            return null;
        }
        ScenarioPreset preset = ScenarioPreset.fromValue(requestContext.getScenario());
        if (preset == ScenarioPreset.TIMEOUT || preset == ScenarioPreset.RETRY_EXHAUSTED) {
            return Boolean.TRUE;
        }
        return null;
    }

    private String resolveVendorCode(SdkErrorCode errorCode, String scenario) {
        ScenarioPreset preset = ScenarioPreset.fromValue(scenario);
        if (preset == ScenarioPreset.INTERNAL_ERROR || errorCode == SdkErrorCode.ERR_VENDOR_INTERNAL) {
            return "VENDOR_INTERNAL";
        }
        if (preset == ScenarioPreset.TIMEOUT || preset == ScenarioPreset.RETRY_EXHAUSTED
                || errorCode == SdkErrorCode.ERR_NETWORK_TIMEOUT
                || errorCode == SdkErrorCode.RETRY_EXHAUSTED) {
            return "VENDOR_TIMEOUT";
        }
        return null;
    }

    private boolean isFinalAttempt(SdkErrorCode errorCode, int attempt) {
        return !RetryHandler.isRetryable(errorCode) || attempt > RetryHandler.getMaxRetry();
    }

    private int extractRetryCount(String resultJson) {
        try {
            return Math.max(0, new JSONObject(resultJson).optInt("retryCount", 0));
        } catch (Exception e) {
            return 0;
        }
    }

    static String buildDiagnosticsPayload(String activePreset, DiagnosticsSnapshot snapshot) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("activePreset", ScenarioPreset.fromValue(activePreset).getValue());

            JSONArray presets = new JSONArray();
            for (String preset : availablePresetValues()) {
                presets.put(preset);
            }
            payload.put("availablePresets", presets);
            payload.put(
                    "snapshot",
                    snapshot == null
                            ? DiagnosticsSnapshot.create(new ArrayList<RequestTimeline>(), Instant.now().toString()).toJsonObject()
                            : snapshot.toJsonObject()
            );
            return payload.toString();
        } catch (JSONException e) {
            return "{\"activePreset\":\"" + ScenarioPreset.fromValue(activePreset).getValue() + "\"}";
        }
    }

    private static List<String> availablePresetValues() {
        List<String> values = new ArrayList<>();
        for (ScenarioPreset preset : ScenarioPreset.values()) {
            values.add(preset.getValue());
        }
        return values;
    }

    private static final class RequestContext {
        private final String method;
        private final String callbackId;
        private final String correlationId;
        private final String scenario;
        private final long startedAtMs;

        private RequestContext(String method, String callbackId, String correlationId, String scenario, long startedAtMs) {
            this.method = method;
            this.callbackId = callbackId;
            this.correlationId = correlationId;
            this.scenario = scenario;
            this.startedAtMs = startedAtMs;
        }

        static RequestContext forMethod(String method, String paramsJson, String scenario) {
            String callbackId = null;
            if (paramsJson != null && !paramsJson.trim().isEmpty()) {
                try {
                    JSONObject params = new JSONObject(paramsJson);
                    callbackId = params.optString("callbackId", null);
                } catch (JSONException ignored) {
                    callbackId = null;
                }
            }
            return new RequestContext(
                    method,
                    callbackId,
                    UUID.randomUUID().toString(),
                    ScenarioPreset.fromValue(scenario).getValue(),
                    System.currentTimeMillis()
            );
        }

        String getMethod() {
            return method;
        }

        String getCallbackId() {
            return callbackId;
        }

        String getCorrelationId() {
            return correlationId;
        }

        String getScenario() {
            return scenario;
        }

        long elapsedMs() {
            return Math.max(0L, System.currentTimeMillis() - startedAtMs);
        }
    }
}
