package com.demo.railbridge.kotlin

import com.demo.railbridge.bridge.InFlightRequestRecord
import com.demo.railbridge.logging.DiagnosticsSnapshot
import com.demo.railbridge.logging.RequestTimeline
import com.demo.railbridge.logging.TimelineEvent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KotlinNativeBridgeDiagnosticsTest {

    @Test
    fun diagnosticsPayload_exposesActivePresetAndIncompleteTimeline() {
        val timeline = RequestTimeline("corr-loss", "requestCharge", "callback_loss")
        timeline.appendEvent(
            TimelineEvent.stage(
                "corr-loss",
                "requestCharge",
                "callback_loss",
                "js_entry",
                "2026-04-06T15:00:00Z",
                0,
                0L,
                null,
                null
            )
        )
        timeline.appendEvent(
            TimelineEvent.stage(
                "corr-loss",
                "requestCharge",
                "callback_loss",
                "native_validation",
                "2026-04-06T15:00:01Z",
                0,
                4L,
                null,
                null
            )
        )
        timeline.appendEvent(
            TimelineEvent.stage(
                "corr-loss",
                "requestCharge",
                "callback_loss",
                "sdk_start",
                "2026-04-06T15:00:02Z",
                0,
                12L,
                null,
                null
            )
        )

        val snapshot = DiagnosticsSnapshot.create(listOf(timeline), "2026-04-06T15:00:03Z")
        val payload = JSONObject(KotlinNativeBridge.buildDiagnosticsPayload("callback_loss", snapshot))
        val snapshotJson = payload.getJSONObject("snapshot")
        val firstTimeline = snapshotJson.getJSONArray("timelines").getJSONObject(0)
        val availablePresets = payload.getJSONArray("availablePresets")

        assertEquals("callback_loss", payload.getString("activePreset"))
        assertEquals(1, snapshotJson.getInt("schemaVersion"))
        assertEquals("incomplete", firstTimeline.getString("finalStatus"))
        assertEquals("sdk_start", firstTimeline.getJSONArray("events").getJSONObject(2).getString("stage"))
        assertFalse(firstTimeline.has("resolvedByRetry"))
        assertEquals("normal", availablePresets.getString(0))
        assertEquals("retry_exhausted", availablePresets.getString(5))
    }

    @Test
    fun diagnosticsPayload_includesExplicitInFlightRequests() {
        val snapshot = DiagnosticsSnapshot.create(emptyList(), "2026-04-08T11:00:00Z")
        val payload = JSONObject(
            KotlinNativeBridge.buildDiagnosticsPayload(
                "callback_loss",
                snapshot,
                listOf(
                    InFlightRequestRecord(
                        "corr-inflight",
                        "requestCharge",
                        "cb-42",
                        "pending",
                        "callback_loss",
                        "2026-04-08T10:59:58Z",
                        850L
                    )
                )
            )
        )
        val inFlight = payload.getJSONArray("inFlightRequests").getJSONObject(0)

        assertEquals(1, payload.getJSONArray("inFlightRequests").length())
        assertEquals("corr-inflight", inFlight.getString("correlationId"))
        assertEquals("requestCharge", inFlight.getString("method"))
        assertEquals("cb-42", inFlight.getString("callbackId"))
        assertEquals("pending", inFlight.getString("state"))
        assertEquals("callback_loss", inFlight.getString("scenario"))
        assertEquals(850L, inFlight.getLong("elapsedMs"))
    }
}
