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
