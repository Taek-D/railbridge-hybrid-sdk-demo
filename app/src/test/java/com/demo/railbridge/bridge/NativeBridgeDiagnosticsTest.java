package com.demo.railbridge.bridge;

import com.demo.railbridge.logging.DiagnosticsSnapshot;
import com.demo.railbridge.logging.RequestTimeline;
import com.demo.railbridge.logging.TimelineEvent;
import com.demo.railbridge.sdk.ChargeResult;
import com.demo.railbridge.sdk.SdkErrorCode;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeBridgeDiagnosticsTest {

    @Test
    public void successPayload_keepsBaseFieldsAndAddsOptionalDiagnosticsMetadata() throws Exception {
        BridgeResponseFactory.Metadata metadata = new BridgeResponseFactory.Metadata(
                "callback-1",
                "corr-1",
                "android",
                "js_callback",
                512L,
                "timeout",
                "VENDOR_TIMEOUT",
                true,
                true
        );

        String json = BridgeResponseFactory.buildChargeSuccess(
                new ChargeResult("txn-1", 1200, 8800, "2026-04-06T15:00:00Z"),
                metadata
        );

        JSONObject payload = new JSONObject(json);

        assertEquals("success", payload.getString("status"));
        assertEquals("requestCharge", payload.getString("method"));
        assertEquals("txn-1", payload.getJSONObject("data").getString("transactionId"));
        assertEquals("callback-1", payload.getString("callbackId"));
        assertEquals("corr-1", payload.getString("correlationId"));
        assertEquals("android", payload.getString("platform"));
        assertEquals("js_callback", payload.getString("stage"));
        assertEquals(512L, payload.getLong("durationMs"));
        assertEquals("timeout", payload.getString("scenario"));
        assertEquals("VENDOR_TIMEOUT", payload.getString("vendorCode"));
        assertTrue(payload.getBoolean("retryable"));
        assertTrue(payload.getBoolean("resolvedByRetry"));
    }

    @Test
    public void diagnosticsPayload_exposesActivePresetAndIncompleteTimeline() throws Exception {
        RequestTimeline timeline = new RequestTimeline("corr-loss", "requestCharge", "callback_loss");
        timeline.appendEvent(TimelineEvent.stage(
                "corr-loss",
                "requestCharge",
                "callback_loss",
                "js_entry",
                "2026-04-06T15:00:00Z",
                0,
                0L,
                null,
                null
        ));
        timeline.appendEvent(TimelineEvent.stage(
                "corr-loss",
                "requestCharge",
                "callback_loss",
                "native_validation",
                "2026-04-06T15:00:01Z",
                0,
                4L,
                null,
                null
        ));
        timeline.appendEvent(TimelineEvent.stage(
                "corr-loss",
                "requestCharge",
                "callback_loss",
                "sdk_start",
                "2026-04-06T15:00:02Z",
                0,
                12L,
                null,
                null
        ));

        DiagnosticsSnapshot snapshot = DiagnosticsSnapshot.create(
                Collections.singletonList(timeline),
                "2026-04-06T15:00:03Z"
        );

        JSONObject payload = new JSONObject(
                NativeBridge.buildDiagnosticsPayload("callback_loss", snapshot)
        );
        JSONObject snapshotJson = payload.getJSONObject("snapshot");
        JSONObject firstTimeline = snapshotJson.getJSONArray("timelines").getJSONObject(0);
        JSONArray availablePresets = payload.getJSONArray("availablePresets");

        assertEquals("callback_loss", payload.getString("activePreset"));
        assertEquals(1, snapshotJson.getInt("schemaVersion"));
        assertEquals("incomplete", firstTimeline.getString("finalStatus"));
        assertEquals("sdk_start", firstTimeline.getJSONArray("events").getJSONObject(2).getString("stage"));
        assertFalse(firstTimeline.has("resolvedByRetry"));
        assertEquals("normal", availablePresets.getString(0));
        assertEquals("retry_exhausted", availablePresets.getString(5));
    }
}
