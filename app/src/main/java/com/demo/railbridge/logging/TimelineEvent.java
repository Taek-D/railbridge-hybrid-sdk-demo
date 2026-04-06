package com.demo.railbridge.logging;

import org.json.JSONException;
import org.json.JSONObject;

public final class TimelineEvent {

    private final String correlationId;
    private final String method;
    private final String scenario;
    private final String stage;
    private final String timestamp;
    private final int retryCount;
    private final Long durationMs;
    private final String vendorCode;
    private final Boolean retryable;
    private final String finalStatus;
    private final Boolean resolvedByRetry;

    private TimelineEvent(
            String correlationId,
            String method,
            String scenario,
            String stage,
            String timestamp,
            int retryCount,
            Long durationMs,
            String vendorCode,
            Boolean retryable,
            String finalStatus,
            Boolean resolvedByRetry
    ) {
        this.correlationId = correlationId;
        this.method = method;
        this.scenario = scenario;
        this.stage = stage;
        this.timestamp = timestamp;
        this.retryCount = retryCount;
        this.durationMs = durationMs;
        this.vendorCode = vendorCode;
        this.retryable = retryable;
        this.finalStatus = finalStatus;
        this.resolvedByRetry = resolvedByRetry;
    }

    public static TimelineEvent stage(
            String correlationId,
            String method,
            String scenario,
            String stage,
            String timestamp,
            int retryCount,
            Long durationMs,
            String vendorCode,
            Boolean retryable
    ) {
        return new TimelineEvent(
                correlationId,
                method,
                scenario,
                stage,
                timestamp,
                Math.max(0, retryCount),
                durationMs,
                vendorCode,
                retryable,
                null,
                null
        );
    }

    public TimelineEvent withCompletion(String finalStatus, boolean resolvedByRetry) {
        return new TimelineEvent(
                correlationId,
                method,
                scenario,
                stage,
                timestamp,
                retryCount,
                durationMs,
                vendorCode,
                retryable,
                finalStatus,
                resolvedByRetry
        );
    }

    public JSONObject toJsonObject() {
        try {
            JSONObject object = new JSONObject();
            object.put("correlationId", correlationId);
            object.put("method", method);
            object.put("scenario", scenario == null ? "" : scenario);
            object.put("stage", stage);
            object.put("timestamp", timestamp);
            object.put("retryCount", retryCount);
            if (durationMs != null) {
                object.put("durationMs", durationMs);
            }
            if (vendorCode != null && !vendorCode.trim().isEmpty()) {
                object.put("vendorCode", vendorCode);
            }
            if (retryable != null) {
                object.put("retryable", retryable);
            }
            if (finalStatus != null && !finalStatus.trim().isEmpty()) {
                object.put("finalStatus", finalStatus);
            }
            if (resolvedByRetry != null) {
                object.put("resolvedByRetry", resolvedByRetry);
            }
            return object;
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize TimelineEvent", e);
        }
    }

    public static TimelineEvent fromJsonObject(JSONObject object) {
        return new TimelineEvent(
                object.optString("correlationId", ""),
                object.optString("method", ""),
                object.optString("scenario", ""),
                object.optString("stage", ""),
                object.optString("timestamp", ""),
                Math.max(0, object.optInt("retryCount", 0)),
                object.has("durationMs") ? Long.valueOf(object.optLong("durationMs")) : null,
                object.optString("vendorCode", null),
                object.has("retryable") ? object.optBoolean("retryable") : null,
                object.optString("finalStatus", null),
                object.has("resolvedByRetry") ? object.optBoolean("resolvedByRetry") : null
        );
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getMethod() {
        return method;
    }

    public String getScenario() {
        return scenario;
    }

    public String getStage() {
        return stage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public Boolean getResolvedByRetry() {
        return resolvedByRetry;
    }
}
