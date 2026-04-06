package com.demo.railbridge.logging;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiagnosticsExportTest {

    @Test
    public void exportJson_isStructuredByTimelineAndContainsEvidenceFields() throws Exception {
        ErrorLogger logger = new ErrorLogger(new MemoryStorage(), 10, new FixedTimeSource("2026-04-06T15:00:00Z"), ErrorLogger.PlatformLogger.noOp());

        logger.recordEvent(TimelineEvent.stage("corr-export", "requestCharge", "timeout", "js_entry", "2026-04-06T14:59:58Z", 0, 0L, null, null));
        logger.recordEvent(TimelineEvent.stage("corr-export", "requestCharge", "timeout", "native_validation", "2026-04-06T14:59:59Z", 0, 2L, null, null));
        logger.recordEvent(
                TimelineEvent.stage("corr-export", "requestCharge", "timeout", "sdk_callback", "2026-04-06T15:00:00Z", 1, 1400L, "VENDOR_TIMEOUT", true)
                        .withCompletion("success", true)
        );

        JSONObject export = new JSONObject(logger.exportDiagnosticsJson());
        JSONArray timelines = export.getJSONArray("timelines");
        JSONObject timeline = timelines.getJSONObject(0);
        JSONObject event = timeline.getJSONArray("events").getJSONObject(2);

        assertEquals(1, export.getInt("schemaVersion"));
        assertEquals("2026-04-06T15:00:00Z", export.getString("exportedAt"));
        assertEquals("corr-export", timeline.getString("correlationId"));
        assertEquals("requestCharge", timeline.getString("method"));
        assertEquals("timeout", timeline.getString("scenario"));
        assertEquals("success", timeline.getString("finalStatus"));
        assertEquals(1, timeline.getInt("finalRetryCount"));
        assertTrue(timeline.getBoolean("resolvedByRetry"));
        assertEquals("sdk_callback", event.getString("stage"));
        assertEquals(1, event.getInt("retryCount"));
        assertEquals(1400L, event.getLong("durationMs"));
        assertEquals("VENDOR_TIMEOUT", event.getString("vendorCode"));
        assertTrue(event.getBoolean("retryable"));
    }

    @Test
    public void exportJson_remainsParseableWhenStorageContainsLegacyFlatLogs() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        storage.value = "[{\"id\":\"log-2\",\"timestamp\":\"2026-04-06T15:10:00Z\",\"method\":\"getBalance\",\"errorCode\":\"9001\",\"errorMessage\":\"Retry attempts exhausted\",\"context\":\"correlationId=corr-old\",\"resolved\":false}]";
        ErrorLogger logger = new ErrorLogger(storage, 10, new FixedTimeSource("2026-04-06T15:20:00Z"), ErrorLogger.PlatformLogger.noOp());

        JSONObject export = new JSONObject(logger.exportDiagnosticsJson());
        JSONObject timeline = export.getJSONArray("timelines").getJSONObject(0);

        assertEquals("corr-old", timeline.getString("correlationId"));
        assertEquals("legacy_log", timeline.getJSONArray("events").getJSONObject(0).getString("stage"));
    }

    private static final class MemoryStorage implements ErrorLogger.Storage {
        private String value;

        @Override
        public String read() {
            return value;
        }

        @Override
        public void write(String value) {
            this.value = value;
        }

        @Override
        public void clear() {
            value = null;
        }
    }

    private static final class FixedTimeSource implements ErrorLogger.TimeSource {
        private final String now;

        private FixedTimeSource(String now) {
            this.now = now;
        }

        @Override
        public String now() {
            return now;
        }
    }
}
