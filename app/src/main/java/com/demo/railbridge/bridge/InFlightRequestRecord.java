package com.demo.railbridge.bridge;

import org.json.JSONException;
import org.json.JSONObject;

public final class InFlightRequestRecord {

    private final String correlationId;
    private final String method;
    private final String callbackId;
    private final String state;
    private final String scenario;
    private final String startedAt;
    private final long elapsedMs;

    public InFlightRequestRecord(
            String correlationId,
            String method,
            String callbackId,
            String state,
            String scenario,
            String startedAt,
            long elapsedMs
    ) {
        this.correlationId = correlationId;
        this.method = method;
        this.callbackId = callbackId;
        this.state = state;
        this.scenario = scenario;
        this.startedAt = startedAt;
        this.elapsedMs = Math.max(0L, elapsedMs);
    }

    public JSONObject toJsonObject() {
        try {
            JSONObject object = new JSONObject();
            object.put("correlationId", correlationId == null ? "" : correlationId);
            object.put("method", method == null ? "" : method);
            if (callbackId != null && !callbackId.trim().isEmpty()) {
                object.put("callbackId", callbackId);
            }
            object.put("state", state == null ? "" : state);
            object.put("scenario", scenario == null ? "" : scenario);
            object.put("startedAt", startedAt == null ? "" : startedAt);
            object.put("elapsedMs", elapsedMs);
            return object;
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize in-flight request", e);
        }
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getMethod() {
        return method;
    }

    public String getCallbackId() {
        return callbackId;
    }

    public String getState() {
        return state;
    }

    public String getScenario() {
        return scenario;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
