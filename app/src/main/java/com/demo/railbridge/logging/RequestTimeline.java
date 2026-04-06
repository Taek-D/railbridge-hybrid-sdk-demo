package com.demo.railbridge.logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RequestTimeline {

    private final String correlationId;
    private final List<TimelineEvent> events = new ArrayList<>();

    private String method;
    private String scenario;
    private String finalStatus = "incomplete";
    private boolean resolvedByRetry;
    private int finalRetryCount;
    private String vendorCode;
    private Boolean retryable;
    private String updatedAt;

    public RequestTimeline(String correlationId, String method, String scenario) {
        this.correlationId = correlationId;
        this.method = method;
        this.scenario = scenario;
    }

    public void appendEvent(TimelineEvent event) {
        events.add(event);

        if (hasText(event.getMethod())) {
            method = event.getMethod();
        }
        if (hasText(event.getScenario())) {
            scenario = event.getScenario();
        }
        if (hasText(event.getVendorCode())) {
            vendorCode = event.getVendorCode();
        }
        if (event.getRetryable() != null) {
            retryable = event.getRetryable();
        }
        finalRetryCount = Math.max(finalRetryCount, event.getRetryCount());
        updatedAt = event.getTimestamp();

        if (hasText(event.getFinalStatus())) {
            finalStatus = event.getFinalStatus();
            if (event.getResolvedByRetry() != null) {
                resolvedByRetry = event.getResolvedByRetry();
            } else {
                resolvedByRetry = "success".equals(finalStatus) && finalRetryCount > 0;
            }
        }
    }

    public JSONObject toJsonObject() {
        try {
            JSONObject object = new JSONObject();
            object.put("correlationId", correlationId);
            object.put("method", method == null ? "" : method);
            object.put("scenario", scenario == null ? "" : scenario);
            object.put("finalStatus", finalStatus);
            object.put("finalRetryCount", finalRetryCount);
            if (resolvedByRetry || finalRetryCount > 0) {
                object.put("resolvedByRetry", resolvedByRetry);
            }
            if (hasText(vendorCode)) {
                object.put("vendorCode", vendorCode);
            }
            if (retryable != null) {
                object.put("retryable", retryable);
            }
            JSONArray eventArray = new JSONArray();
            for (TimelineEvent event : events) {
                eventArray.put(event.toJsonObject());
            }
            object.put("events", eventArray);
            return object;
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize RequestTimeline", e);
        }
    }

    public static RequestTimeline fromJsonObject(JSONObject object) {
        RequestTimeline timeline = new RequestTimeline(
                object.optString("correlationId", ""),
                object.optString("method", ""),
                object.optString("scenario", "")
        );

        JSONArray eventArray = object.optJSONArray("events");
        if (eventArray != null) {
            for (int i = 0; i < eventArray.length(); i++) {
                JSONObject eventObject = eventArray.optJSONObject(i);
                if (eventObject != null) {
                    timeline.appendEvent(TimelineEvent.fromJsonObject(eventObject));
                }
            }
        }

        timeline.finalStatus = object.optString("finalStatus", timeline.finalStatus);
        timeline.finalRetryCount = Math.max(timeline.finalRetryCount, object.optInt("finalRetryCount", timeline.finalRetryCount));
        timeline.resolvedByRetry = object.optBoolean("resolvedByRetry", timeline.resolvedByRetry);
        timeline.vendorCode = object.optString("vendorCode", timeline.vendorCode);
        if (object.has("retryable")) {
            timeline.retryable = object.optBoolean("retryable");
        }
        return timeline;
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

    public String getFinalStatus() {
        return finalStatus;
    }

    public boolean isResolvedByRetry() {
        return resolvedByRetry;
    }

    public int getFinalRetryCount() {
        return finalRetryCount;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public List<TimelineEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
