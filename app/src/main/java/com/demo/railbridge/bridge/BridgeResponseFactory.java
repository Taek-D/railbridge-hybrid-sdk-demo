package com.demo.railbridge.bridge;

import com.demo.railbridge.sdk.BalanceSnapshot;
import com.demo.railbridge.sdk.ChargeResult;
import com.demo.railbridge.sdk.SdkErrorCode;
import com.demo.railbridge.sdk.SdkStatusSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

public final class BridgeResponseFactory {

    private BridgeResponseFactory() {
    }

    public static String buildChargeSuccess(ChargeResult result, Metadata metadata) {
        try {
            JSONObject data = new JSONObject();
            data.put("transactionId", result.getTransactionId());
            data.put("amount", result.getAmount());
            data.put("balance", result.getBalance());
            data.put("timestamp", result.getTimestamp());
            return buildSuccess("requestCharge", data, 0, metadata);
        } catch (JSONException e) {
            return buildError("requestCharge", SdkErrorCode.ERR_UNKNOWN, 0, null, null, null, metadata);
        }
    }

    public static String buildBalanceSuccess(BalanceSnapshot result, Metadata metadata) {
        try {
            JSONObject data = new JSONObject();
            data.put("cardId", result.getCardId());
            data.put("balance", result.getBalance());
            data.put("timestamp", result.getTimestamp());
            return buildSuccess("getBalance", data, 0, metadata);
        } catch (JSONException e) {
            return buildError("getBalance", SdkErrorCode.ERR_UNKNOWN, 0, null, null, null, metadata);
        }
    }

    public static String buildStatusSuccess(SdkStatusSnapshot result, Metadata metadata) {
        try {
            JSONObject data = new JSONObject();
            data.put("initialized", result.isInitialized());
            data.put("version", result.getVersion());
            return buildSuccess("getSdkStatus", data, 0, metadata);
        } catch (JSONException e) {
            return buildError("getSdkStatus", SdkErrorCode.ERR_UNKNOWN, 0, null, null, null, metadata);
        }
    }

    public static String buildReportAck(String code, String message, Metadata metadata) {
        try {
            JSONObject response = new JSONObject();
            response.put("status", "received");
            response.put("message", message);
            response.put("code", code);
            addMetadata(response, metadata);
            return response.toString();
        } catch (JSONException e) {
            return "{\"status\":\"received\",\"message\":\"Error report received\"}";
        }
    }

    public static String buildError(
            String method,
            SdkErrorCode errorCode,
            int retryCount,
            String logId,
            String vendorCode,
            Boolean retryable,
            Metadata metadata
    ) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", errorCode.getCode());
            error.put("message", errorCode.getMessage());
            error.put("retryCount", retryCount);
            error.put("resolved", false);

            if (vendorCode != null && !vendorCode.trim().isEmpty()) {
                error.put("vendorCode", vendorCode);
            }
            if (retryable != null) {
                error.put("retryable", retryable);
            }

            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("method", method);
            response.put("error", error);
            if (logId != null && !logId.trim().isEmpty()) {
                response.put("logId", logId);
            }
            addMetadata(response, metadata);
            return response.toString();
        } catch (JSONException e) {
            return "{\"status\":\"error\",\"method\":\"" + method + "\"}";
        }
    }

    private static String buildSuccess(String method, JSONObject data, int retryCount, Metadata metadata)
            throws JSONException {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("method", method);
        response.put("data", data);
        response.put("retryCount", retryCount);
        addMetadata(response, metadata);
        return response.toString();
    }

    private static void addMetadata(JSONObject response, Metadata metadata) throws JSONException {
        if (metadata == null) {
            return;
        }
        if (hasText(metadata.getCallbackId())) {
            response.put("callbackId", metadata.getCallbackId());
        }
        if (hasText(metadata.getCorrelationId())) {
            response.put("correlationId", metadata.getCorrelationId());
        }
        if (hasText(metadata.getPlatform())) {
            response.put("platform", metadata.getPlatform());
        }
        if (hasText(metadata.getStage())) {
            response.put("stage", metadata.getStage());
        }
        if (metadata.getDurationMs() != null) {
            response.put("durationMs", metadata.getDurationMs());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static final class Metadata {

        private final String callbackId;
        private final String correlationId;
        private final String platform;
        private final String stage;
        private final Long durationMs;

        public Metadata(
                String callbackId,
                String correlationId,
                String platform,
                String stage,
                Long durationMs
        ) {
            this.callbackId = callbackId;
            this.correlationId = correlationId;
            this.platform = platform;
            this.stage = stage;
            this.durationMs = durationMs;
        }

        public String getCallbackId() {
            return callbackId;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public String getPlatform() {
            return platform;
        }

        public String getStage() {
            return stage;
        }

        public Long getDurationMs() {
            return durationMs;
        }
    }
}
