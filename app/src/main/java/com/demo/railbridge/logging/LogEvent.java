package com.demo.railbridge.logging;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.UUID;

public class LogEvent {

    private final String id;
    private final String timestamp;
    private final String method;
    private final String errorCode;
    private final String errorMessage;
    private final String context;
    private boolean resolved;

    public LogEvent(String method, String errorCode, String errorMessage, String context) {
        this(UUID.randomUUID().toString(), Instant.now().toString(), method, errorCode, errorMessage, context, false);
    }

    private LogEvent(
            String id,
            String timestamp,
            String method,
            String errorCode,
            String errorMessage,
            String context,
            boolean resolved
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.method = method;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.context = context;
        this.resolved = resolved;
    }

    public void markResolved() {
        resolved = true;
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMethod() {
        return method;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getContext() {
        return context;
    }

    public boolean isResolved() {
        return resolved;
    }

    public JSONObject toJsonObject() {
        try {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("timestamp", timestamp);
            object.put("method", method);
            object.put("errorCode", errorCode);
            object.put("errorMessage", errorMessage);
            object.put("context", context == null ? "" : context);
            object.put("resolved", resolved);
            return object;
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize LogEvent", e);
        }
    }

    public static LogEvent fromJsonObject(JSONObject object) {
        return new LogEvent(
                object.optString("id", UUID.randomUUID().toString()),
                object.optString("timestamp", Instant.now().toString()),
                object.optString("method", ""),
                object.optString("errorCode", ""),
                object.optString("errorMessage", ""),
                object.optString("context", ""),
                object.optBoolean("resolved", false)
        );
    }

    public static LogEvent fromTimeline(RequestTimeline timeline) {
        String finalStatus = timeline.getFinalStatus();
        String message;
        String errorCode;

        if ("success".equals(finalStatus)) {
            message = timeline.isResolvedByRetry() ? "Recovered after retry" : "Completed successfully";
            errorCode = "RESOLVED";
        } else if ("incomplete".equals(finalStatus)) {
            message = "Request incomplete";
            errorCode = "INCOMPLETE";
        } else {
            message = "Request failed";
            errorCode = "ERROR";
        }

        JSONObject contextObject = new JSONObject();
        try {
            contextObject.put("correlationId", timeline.getCorrelationId());
            contextObject.put("scenario", timeline.getScenario() == null ? "" : timeline.getScenario());
            contextObject.put("finalStatus", finalStatus);
            contextObject.put("finalRetryCount", timeline.getFinalRetryCount());
            if (timeline.getVendorCode() != null && !timeline.getVendorCode().trim().isEmpty()) {
                contextObject.put("vendorCode", timeline.getVendorCode());
            }
            if (timeline.getRetryable() != null) {
                contextObject.put("retryable", timeline.getRetryable());
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize legacy context", e);
        }

        return new LogEvent(
                timeline.getCorrelationId(),
                timeline.getUpdatedAt() == null ? Instant.now().toString() : timeline.getUpdatedAt(),
                timeline.getMethod(),
                errorCode,
                message,
                contextObject.toString(),
                timeline.isResolvedByRetry() || "success".equals(finalStatus)
        );
    }

    @Override
    public String toString() {
        return "LogEvent{"
                + "id='" + id + '\''
                + ", timestamp='" + timestamp + '\''
                + ", method='" + method + '\''
                + ", errorCode='" + errorCode + '\''
                + ", errorMessage='" + errorMessage + '\''
                + ", context='" + context + '\''
                + ", resolved=" + resolved
                + '}';
    }
}
