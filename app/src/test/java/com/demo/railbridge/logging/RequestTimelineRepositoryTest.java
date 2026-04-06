package com.demo.railbridge.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequestTimelineRepositoryTest {

    @Test
    public void recordEvent_groupsStagesByCorrelationIdAndPreservesOrder() {
        MemoryStorage storage = new MemoryStorage();
        ErrorLogger logger = new ErrorLogger(storage, 10, new FixedTimeSource("2026-04-06T12:00:00Z"), ErrorLogger.PlatformLogger.noOp());

        logger.recordEvent(TimelineEvent.stage("corr-1", "requestCharge", "timeout", "js_entry", "2026-04-06T12:00:01Z", 0, 0L, null, null));
        logger.recordEvent(TimelineEvent.stage("corr-1", "requestCharge", "timeout", "native_validation", "2026-04-06T12:00:02Z", 0, 2L, null, null));
        logger.recordEvent(TimelineEvent.stage("corr-1", "requestCharge", "timeout", "sdk_start", "2026-04-06T12:00:03Z", 0, 3L, null, null));
        logger.recordEvent(
                TimelineEvent.stage("corr-1", "requestCharge", "timeout", "sdk_callback", "2026-04-06T12:00:04Z", 1, 1200L, "VENDOR_TIMEOUT", true)
                        .withCompletion("error", false)
        );

        DiagnosticsSnapshot snapshot = logger.getDiagnosticsSnapshot();
        RequestTimeline timeline = snapshot.getTimelines().get(0);

        assertEquals(1, snapshot.getTimelines().size());
        assertEquals("corr-1", timeline.getCorrelationId());
        assertEquals("requestCharge", timeline.getMethod());
        assertEquals("timeout", timeline.getScenario());
        assertEquals("error", timeline.getFinalStatus());
        assertEquals(1, timeline.getFinalRetryCount());
        assertEquals("VENDOR_TIMEOUT", timeline.getVendorCode());
        assertEquals(Boolean.TRUE, timeline.getRetryable());
        assertEquals(4, timeline.getEvents().size());
        assertEquals("js_entry", timeline.getEvents().get(0).getStage());
        assertEquals("native_validation", timeline.getEvents().get(1).getStage());
        assertEquals("sdk_start", timeline.getEvents().get(2).getStage());
        assertEquals("sdk_callback", timeline.getEvents().get(3).getStage());
    }

    @Test
    public void snapshot_keepsIncompleteRequestsVisible() {
        ErrorLogger logger = new ErrorLogger(new MemoryStorage(), 10, new FixedTimeSource("2026-04-06T12:30:00Z"), ErrorLogger.PlatformLogger.noOp());

        logger.recordEvent(TimelineEvent.stage("corr-2", "getBalance", "callback_loss", "js_entry", "2026-04-06T12:30:01Z", 0, 0L, null, null));
        logger.recordEvent(TimelineEvent.stage("corr-2", "getBalance", "callback_loss", "native_validation", "2026-04-06T12:30:02Z", 0, 1L, null, null));
        logger.recordEvent(TimelineEvent.stage("corr-2", "getBalance", "callback_loss", "sdk_start", "2026-04-06T12:30:03Z", 0, 4L, null, null));

        DiagnosticsSnapshot snapshot = logger.getDiagnosticsSnapshot();
        RequestTimeline timeline = snapshot.getTimelines().get(0);

        assertEquals(1, snapshot.getTimelines().size());
        assertEquals("incomplete", timeline.getFinalStatus());
        assertEquals(3, timeline.getEvents().size());
        assertFalse(timeline.isResolvedByRetry());
    }

    @Test
    public void retentionLimit_keepsNewestTimelines() {
        ErrorLogger logger = new ErrorLogger(new MemoryStorage(), 2, new FixedTimeSource("2026-04-06T13:00:00Z"), ErrorLogger.PlatformLogger.noOp());

        logger.recordEvent(TimelineEvent.stage("corr-1", "requestCharge", "normal", "sdk_callback", "2026-04-06T13:00:01Z", 0, 20L, null, false).withCompletion("success", false));
        logger.recordEvent(TimelineEvent.stage("corr-2", "getBalance", "normal", "sdk_callback", "2026-04-06T13:00:02Z", 0, 10L, null, false).withCompletion("success", false));
        logger.recordEvent(TimelineEvent.stage("corr-3", "requestCharge", "timeout", "sdk_callback", "2026-04-06T13:00:03Z", 1, 800L, "VENDOR_TIMEOUT", true).withCompletion("error", false));

        DiagnosticsSnapshot snapshot = logger.getDiagnosticsSnapshot();

        assertEquals(2, snapshot.getTimelines().size());
        assertEquals("corr-3", snapshot.getTimelines().get(0).getCorrelationId());
        assertEquals("corr-2", snapshot.getTimelines().get(1).getCorrelationId());
    }

    @Test
    public void legacyFlatLogPayload_isMigratedInsteadOfDropped() {
        MemoryStorage storage = new MemoryStorage();
        storage.value = "[{\"id\":\"log-1\",\"timestamp\":\"2026-04-06T14:00:00Z\",\"method\":\"requestCharge\",\"errorCode\":\"1002\",\"errorMessage\":\"Network timeout\",\"context\":\"{\\\"correlationId\\\":\\\"corr-legacy\\\"}\",\"resolved\":false}]";
        ErrorLogger logger = new ErrorLogger(storage, 10, new FixedTimeSource("2026-04-06T14:05:00Z"), ErrorLogger.PlatformLogger.noOp());

        DiagnosticsSnapshot snapshot = logger.getDiagnosticsSnapshot();
        RequestTimeline timeline = snapshot.getTimelines().get(0);

        assertEquals(1, snapshot.getTimelines().size());
        assertEquals("corr-legacy", timeline.getCorrelationId());
        assertEquals("legacy", timeline.getScenario());
        assertEquals("error", timeline.getFinalStatus());
        assertEquals("legacy_log", timeline.getEvents().get(0).getStage());
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
