package com.demo.railbridge.bridge;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.demo.railbridge.logging.ErrorLogger;
import com.demo.railbridge.retry.RetryHandler;
import com.demo.railbridge.sdk.ChargeResult;
import com.demo.railbridge.sdk.MockRailSdk;
import com.demo.railbridge.sdk.SdkErrorCode;

import org.json.JSONException;
import org.json.JSONObject;

public class NativeBridge {

    private static final String TAG = "RailBridge.NativeBridge";

    private final WebView webView;
    private final MockRailSdk mockRailSdk;
    private final ErrorLogger errorLogger;
    private final RetryHandler retryHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean destroyed;

    public NativeBridge(WebView webView, MockRailSdk mockRailSdk, ErrorLogger errorLogger) {
        this.webView = webView;
        this.mockRailSdk = mockRailSdk;
        this.errorLogger = errorLogger;
        this.retryHandler = new RetryHandler(errorLogger);
    }

    @JavascriptInterface
    public void requestCharge(String paramsJson) {
        Log.d(TAG, "requestCharge called: " + paramsJson);

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
                    mockRailSdk.charge(cardId, amount, new MockRailSdk.SdkCallback<ChargeResult>() {
                        @Override
                        public void onSuccess(ChargeResult result) {
                            onSuccess.onResult(buildChargeSuccessJson(result));
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
                            "{\"cardId\":\"" + cardId + "\",\"amount\":" + amount + "}"
                    );
                    postResultToJs(buildErrorJson("requestCharge", errorCode, retryCount));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "requestCharge exception", e);
            errorLogger.logSdkFailure(
                    "requestCharge",
                    SdkErrorCode.ERR_UNKNOWN,
                    "params=" + paramsJson + ", error=" + e.getMessage()
            );
            postResultToJs(buildErrorJson("requestCharge", SdkErrorCode.ERR_UNKNOWN, 0));
        }
    }

    @JavascriptInterface
    public void getBalance(String paramsJson) {
        Log.d(TAG, "getBalance called: " + paramsJson);

        try {
            JSONObject params = new JSONObject(paramsJson);
            final String cardId = params.optString("cardId", "");

            retryHandler.execute(new RetryHandler.RetryTask() {
                @Override
                public void run(
                        RetryHandler.OnSuccessCallback onSuccess,
                        RetryHandler.OnErrorCallback onError
                ) {
                    mockRailSdk.getBalance(cardId, new MockRailSdk.SdkCallback<MockRailSdk.BalanceResult>() {
                        @Override
                        public void onSuccess(MockRailSdk.BalanceResult result) {
                            onSuccess.onResult(buildBalanceSuccessJson(result));
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
                            "{\"cardId\":\"" + cardId + "\"}"
                    );
                    postResultToJs(buildErrorJson("getBalance", errorCode, retryCount));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getBalance exception", e);
            errorLogger.logSdkFailure(
                    "getBalance",
                    SdkErrorCode.ERR_UNKNOWN,
                    "params=" + paramsJson + ", error=" + e.getMessage()
            );
            postResultToJs(buildErrorJson("getBalance", SdkErrorCode.ERR_UNKNOWN, 0));
        }
    }

    @JavascriptInterface
    public void getSdkStatus() {
        Log.d(TAG, "getSdkStatus called");

        try {
            JSONObject data = new JSONObject();
            data.put("initialized", mockRailSdk.isInitialized());
            data.put("version", mockRailSdk.getSdkVersion());

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("method", "getSdkStatus");
            response.put("data", data);
            response.put("retryCount", 0);

            postResultToJs(response.toString());
        } catch (JSONException e) {
            Log.e(TAG, "getSdkStatus exception", e);
            postResultToJs(buildErrorJson("getSdkStatus", SdkErrorCode.ERR_UNKNOWN, 0));
        }
    }

    @JavascriptInterface
    public void reportError(String errorJson) {
        Log.d(TAG, "reportError called: " + errorJson);

        try {
            JSONObject error = new JSONObject(errorJson);
            String code = error.optString("code", "UNKNOWN");
            String message = error.optString("message", "");
            String context = error.optString("context", "");

            errorLogger.logSdkFailure(
                    "reportError (from JS)",
                    SdkErrorCode.ERR_UNKNOWN,
                    "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"context\":\"" + context + "\"}"
            );

            JSONObject response = new JSONObject();
            response.put("status", "received");
            response.put("message", "Error report received");
            response.put("code", code);

            postResultToJs(response.toString());
        } catch (Exception e) {
            Log.e(TAG, "reportError exception", e);
            postResultToJs(buildErrorJson("reportError", SdkErrorCode.ERR_UNKNOWN, 0));
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

    private String buildChargeSuccessJson(ChargeResult result) {
        try {
            JSONObject data = new JSONObject();
            data.put("transactionId", result.getTransactionId());
            data.put("amount", result.getAmount());
            data.put("balance", result.getBalance());
            data.put("timestamp", result.getTimestamp());

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("method", "requestCharge");
            response.put("data", data);
            response.put("retryCount", 0);
            return response.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build charge JSON", e);
            return buildErrorJson("requestCharge", SdkErrorCode.ERR_UNKNOWN, 0);
        }
    }

    private String buildBalanceSuccessJson(MockRailSdk.BalanceResult result) {
        try {
            JSONObject data = new JSONObject();
            data.put("cardId", result.getCardId());
            data.put("balance", result.getBalance());
            data.put("timestamp", result.getTimestamp());

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("method", "getBalance");
            response.put("data", data);
            response.put("retryCount", 0);
            return response.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build balance JSON", e);
            return buildErrorJson("getBalance", SdkErrorCode.ERR_UNKNOWN, 0);
        }
    }

    private String buildErrorJson(String method, SdkErrorCode errorCode, int retryCount) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", errorCode.getCode());
            error.put("message", errorCode.getMessage());
            error.put("retryCount", retryCount);
            error.put("resolved", false);

            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("method", method);
            response.put("error", error);
            response.put("logId", java.util.UUID.randomUUID().toString());
            return response.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build error JSON", e);
            return "{\"status\":\"error\",\"method\":\"" + method + "\"}";
        }
    }
}
