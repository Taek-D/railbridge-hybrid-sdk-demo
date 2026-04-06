package com.demo.railbridge.bridge;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.demo.railbridge.logging.ErrorLogger;
import com.demo.railbridge.retry.RetryHandler;
import com.demo.railbridge.sdk.BalanceSnapshot;
import com.demo.railbridge.sdk.ChargeResult;
import com.demo.railbridge.sdk.RailPlusSdkAdapter;
import com.demo.railbridge.sdk.SdkErrorCode;
import com.demo.railbridge.sdk.SdkStatusSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

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
        RequestContext requestContext = RequestContext.forMethod("requestCharge", paramsJson);

        try {
            JSONObject params = new JSONObject(paramsJson);
            final String cardId = params.optString("cardId", "");
            final int amount = params.optInt("amount", 0);

            retryHandler.execute(new RetryHandler.RetryTask() {
                @Override
                public void run(
                        RetryHandler.OnSuccessCallback onSuccess,
                        RetryHandler.OnErrorCallback onError
                ) {
                    sdkAdapter.requestCharge(cardId, amount, new RailPlusSdkAdapter.Callback<ChargeResult>() {
                        @Override
                        public void onSuccess(ChargeResult result) {
                            onSuccess.onResult(BridgeResponseFactory.buildChargeSuccess(
                                    result,
                                    buildMetadata(requestContext, "sdk_success")
                            ));
                        }

                        @Override
                        public void onError(SdkErrorCode errorCode) {
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
                    Log.d(TAG, "requestCharge success: " + resultJson);
                    postResultToJs(resultJson);
                }

                @Override
                public void onError(SdkErrorCode errorCode, int retryCount) {
                    Log.e(TAG, "requestCharge failed: " + errorCode);
                    errorLogger.logSdkFailure(
                            "requestCharge",
                            errorCode,
                            buildFailureContext(cardId, amount, requestContext.getCorrelationId())
                    );
                    postResultToJs(BridgeResponseFactory.buildError(
                            "requestCharge",
                            errorCode,
                            retryCount,
                            UUID.randomUUID().toString(),
                            null,
                            RetryHandler.isRetryable(errorCode),
                            buildMetadata(requestContext, "sdk_error")
                    ));
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
            postResultToJs(BridgeResponseFactory.buildError(
                    "requestCharge",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildMetadata(requestContext, "bridge_exception")
            ));
        }
    }

    @JavascriptInterface
    public void getBalance(String paramsJson) {
        Log.d(TAG, "getBalance called: " + paramsJson);
        RequestContext requestContext = RequestContext.forMethod("getBalance", paramsJson);

        try {
            JSONObject params = new JSONObject(paramsJson);
            final String cardId = params.optString("cardId", "");

            retryHandler.execute(new RetryHandler.RetryTask() {
                @Override
                public void run(
                        RetryHandler.OnSuccessCallback onSuccess,
                        RetryHandler.OnErrorCallback onError
                ) {
                    sdkAdapter.getBalance(cardId, new RailPlusSdkAdapter.Callback<BalanceSnapshot>() {
                        @Override
                        public void onSuccess(BalanceSnapshot result) {
                            onSuccess.onResult(BridgeResponseFactory.buildBalanceSuccess(
                                    result,
                                    buildMetadata(requestContext, "sdk_success")
                            ));
                        }

                        @Override
                        public void onError(SdkErrorCode errorCode) {
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
                    Log.d(TAG, "getBalance success: " + resultJson);
                    postResultToJs(resultJson);
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
                    postResultToJs(BridgeResponseFactory.buildError(
                            "getBalance",
                            errorCode,
                            retryCount,
                            UUID.randomUUID().toString(),
                            null,
                            RetryHandler.isRetryable(errorCode),
                            buildMetadata(requestContext, "sdk_error")
                    ));
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
            postResultToJs(BridgeResponseFactory.buildError(
                    "getBalance",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildMetadata(requestContext, "bridge_exception")
            ));
        }
    }

    @JavascriptInterface
    public void getSdkStatus() {
        Log.d(TAG, "getSdkStatus called");
        RequestContext requestContext = RequestContext.forMethod("getSdkStatus", null);

        try {
            SdkStatusSnapshot status = sdkAdapter.getStatus();
            postResultToJs(BridgeResponseFactory.buildStatusSuccess(
                    status,
                    buildMetadata(requestContext, "status")
            ));
        } catch (Exception e) {
            Log.e(TAG, "getSdkStatus exception", e);
            postResultToJs(BridgeResponseFactory.buildError(
                    "getSdkStatus",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildMetadata(requestContext, "bridge_exception")
            ));
        }
    }

    @JavascriptInterface
    public void reportError(String errorJson) {
        Log.d(TAG, "reportError called: " + errorJson);
        RequestContext requestContext = RequestContext.forMethod("reportError", errorJson);

        try {
            JSONObject error = new JSONObject(errorJson);
            String code = error.optString("code", "UNKNOWN");
            String message = error.optString("message", "");
            String context = error.optString("context", "");

            errorLogger.logSdkFailure(
                    "reportError (from JS)",
                    SdkErrorCode.ERR_UNKNOWN,
                    "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"context\":\"" + context
                            + "\",\"correlationId\":\"" + requestContext.getCorrelationId() + "\"}"
            );

            postResultToJs(BridgeResponseFactory.buildReportAck(
                    code,
                    "Error report received",
                    buildMetadata(requestContext, "ack")
            ));
        } catch (Exception e) {
            Log.e(TAG, "reportError exception", e);
            postResultToJs(BridgeResponseFactory.buildError(
                    "reportError",
                    SdkErrorCode.ERR_UNKNOWN,
                    0,
                    UUID.randomUUID().toString(),
                    null,
                    false,
                    buildMetadata(requestContext, "bridge_exception")
            ));
        }
    }

    public void destroy() {
        destroyed = true;
        retryHandler.cancel();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void postResultToJs(String resultJson) {
        mainHandler.post(() -> {
            if (destroyed) {
                return;
            }
            String jsCode = "window.onBridgeResult(" + JSONObject.quote(resultJson) + ")";
            webView.evaluateJavascript(jsCode, null);
        });
    }

    private BridgeResponseFactory.Metadata buildMetadata(RequestContext requestContext, String stage) {
        return new BridgeResponseFactory.Metadata(
                requestContext.getCallbackId(),
                requestContext.getCorrelationId(),
                PLATFORM_ANDROID,
                stage,
                requestContext.elapsedMs()
        );
    }

    private String buildFailureContext(String cardId, int amount, String correlationId) {
        return "{\"cardId\":\"" + cardId + "\",\"amount\":" + amount
                + ",\"correlationId\":\"" + correlationId + "\"}";
    }

    private static final class RequestContext {
        private final String method;
        private final String callbackId;
        private final String correlationId;
        private final long startedAtMs;

        private RequestContext(String method, String callbackId, String correlationId, long startedAtMs) {
            this.method = method;
            this.callbackId = callbackId;
            this.correlationId = correlationId;
            this.startedAtMs = startedAtMs;
        }

        static RequestContext forMethod(String method, String paramsJson) {
            String callbackId = null;
            if (paramsJson != null && !paramsJson.trim().isEmpty()) {
                try {
                    JSONObject params = new JSONObject(paramsJson);
                    callbackId = params.optString("callbackId", null);
                } catch (JSONException ignored) {
                    callbackId = null;
                }
            }
            return new RequestContext(method, callbackId, UUID.randomUUID().toString(), System.currentTimeMillis());
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

        long elapsedMs() {
            return Math.max(0L, System.currentTimeMillis() - startedAtMs);
        }
    }
}
