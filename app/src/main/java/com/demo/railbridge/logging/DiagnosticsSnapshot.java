package com.demo.railbridge.logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsSnapshot {

    public static final int SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final String exportedAt;
    private final List<RequestTimeline> timelines;

    private DiagnosticsSnapshot(int schemaVersion, String exportedAt, List<RequestTimeline> timelines) {
        this.schemaVersion = schemaVersion;
        this.exportedAt = exportedAt;
        this.timelines = new ArrayList<>(timelines);
    }

    public static DiagnosticsSnapshot create(List<RequestTimeline> timelines, String exportedAt) {
        return new DiagnosticsSnapshot(SCHEMA_VERSION, exportedAt, timelines);
    }

    public JSONObject toJsonObject() {
        try {
            JSONObject object = new JSONObject();
            object.put("schemaVersion", schemaVersion);
            object.put("exportedAt", exportedAt == null ? "" : exportedAt);
            JSONArray timelineArray = new JSONArray();
            for (RequestTimeline timeline : timelines) {
                timelineArray.put(timeline.toJsonObject());
            }
            object.put("timelines", timelineArray);
            return object;
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize DiagnosticsSnapshot", e);
        }
    }

    public String toJsonString() {
        return toJsonObject().toString();
    }

    public static DiagnosticsSnapshot fromJsonObject(JSONObject object) {
        List<RequestTimeline> timelines = new ArrayList<>();
        JSONArray timelineArray = object.optJSONArray("timelines");
        if (timelineArray != null) {
            for (int i = 0; i < timelineArray.length(); i++) {
                JSONObject timelineObject = timelineArray.optJSONObject(i);
                if (timelineObject != null) {
                    timelines.add(RequestTimeline.fromJsonObject(timelineObject));
                }
            }
        }
        return new DiagnosticsSnapshot(
                object.optInt("schemaVersion", SCHEMA_VERSION),
                object.optString("exportedAt", ""),
                timelines
        );
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public List<RequestTimeline> getTimelines() {
        return Collections.unmodifiableList(timelines);
    }
}
