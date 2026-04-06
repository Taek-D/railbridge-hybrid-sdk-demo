package com.demo.railbridge.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.demo.railbridge.sdk.SdkErrorCode;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ErrorLogger {

    private static final String TAG = "RailBridge.ErrorLogger";
    private static final String PREFS_NAME = "railbridge_error_logs";
    private static final String KEY_LOGS = "error_logs";
    private static final int MAX_LOGS = 50;

    private static ErrorLogger instance;

    private final Storage storage;
    private final int maxTimelines;
    private final TimeSource timeSource;
    private final PlatformLogger platformLogger;
    private boolean crashlyticsEnabled;

    private ErrorLogger(Context context) {
        this(
                new SharedPreferencesStorage(
                        context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ),
                MAX_LOGS,
                TimeSource.system(),
                PlatformLogger.android()
        );
    }

    ErrorLogger(Storage storage, int maxTimelines, TimeSource timeSource, PlatformLogger platformLogger) {
        this.storage = storage;
        this.maxTimelines = Math.max(1, maxTimelines);
        this.timeSource = timeSource == null ? TimeSource.system() : timeSource;
        this.platformLogger = platformLogger == null ? PlatformLogger.noOp() : platformLogger;
    }

    public static synchronized ErrorLogger getInstance(Context context) {
        if (instance == null) {
            instance = new ErrorLogger(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public void setCrashlyticsEnabled(boolean enabled) {
        crashlyticsEnabled = enabled;
    }

    public synchronized void log(LogEvent event) {
        platformLogger.error(TAG, event.toString(), null);

        if (crashlyticsEnabled) {
            platformLogger.debug(TAG, "[Crashlytics] Logged: " + event.getId());
        }

        recordEvent(convertLegacyEvent(event));
    }

    public void logSdkFailure(String method, SdkErrorCode code, String context) {
        if (crashlyticsEnabled) {
            platformLogger.debug(TAG, "[Crashlytics] Custom keys set: code=" + code.getCode() + ", method=" + method);
        }
        log(new LogEvent(method, code.getCode(), code.getMessage(), context));
    }

    public void logRetryAttempt(String method, int attempt, SdkErrorCode errorCode) {
        String context = "{\"attempt\":" + attempt + ",\"errorCode\":\"" + errorCode.getCode() + "\"}";
        log(new LogEvent(
                method + " (Retry)",
                errorCode.getCode(),
                errorCode.getMessage() + " (attempt " + attempt + ")",
                context
        ));
    }

    public void logRetrySuccess(String method, int totalAttempts) {
        String context = "{\"totalAttempts\":" + totalAttempts + ",\"resolved\":true}";
        LogEvent event = new LogEvent(
                method + " (Retry Success)",
                "RESOLVED",
                "Recovered after retry",
                context
        );
        event.markResolved();
        log(event);
    }

    public synchronized void recordEvent(TimelineEvent event) {
        if (event == null) {
            return;
        }

        List<RequestTimeline> timelines = new ArrayList<>(readSnapshot().getTimelines());
        RequestTimeline timeline = null;

        for (int i = 0; i < timelines.size(); i++) {
            RequestTimeline candidate = timelines.get(i);
            if (candidate.getCorrelationId().equals(event.getCorrelationId())) {
                timeline = candidate;
                timelines.remove(i);
                break;
            }
        }

        if (timeline == null) {
            timeline = new RequestTimeline(event.getCorrelationId(), event.getMethod(), event.getScenario());
        }

        timeline.appendEvent(event);
        timelines.add(0, timeline);

        if (timelines.size() > maxTimelines) {
            timelines = new ArrayList<>(timelines.subList(0, maxTimelines));
        }

        writeSnapshot(DiagnosticsSnapshot.create(timelines, timeSource.now()));
    }

    public synchronized DiagnosticsSnapshot getDiagnosticsSnapshot() {
        DiagnosticsSnapshot snapshot = readSnapshot();
        return DiagnosticsSnapshot.create(snapshot.getTimelines(), timeSource.now());
    }

    public synchronized String exportDiagnosticsJson() {
        return getDiagnosticsSnapshot().toJsonString();
    }

    public synchronized List<LogEvent> getRecentLogs() {
        List<LogEvent> logs = new ArrayList<>();
        for (RequestTimeline timeline : readSnapshot().getTimelines()) {
            logs.add(LogEvent.fromTimeline(timeline));
        }
        return logs;
    }

    public synchronized void clearLogs() {
        storage.clear();
        platformLogger.info(TAG, "Logs cleared");
    }

    private DiagnosticsSnapshot readSnapshot() {
        String raw = storage.read();
        if (raw == null || raw.trim().isEmpty()) {
            return DiagnosticsSnapshot.create(new ArrayList<RequestTimeline>(), timeSource.now());
        }

        try {
            Object parsed = new JSONTokener(raw).nextValue();
            if (parsed instanceof JSONObject) {
                return DiagnosticsSnapshot.fromJsonObject((JSONObject) parsed);
            }
            if (parsed instanceof JSONArray) {
                return migrateLegacyArray((JSONArray) parsed);
            }
        } catch (Exception e) {
            platformLogger.error(TAG, "Failed to parse logs", e);
        }

        return DiagnosticsSnapshot.create(new ArrayList<RequestTimeline>(), timeSource.now());
    }

    private DiagnosticsSnapshot migrateLegacyArray(JSONArray array) {
        List<RequestTimeline> timelines = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                continue;
            }

            LogEvent legacyEvent = LogEvent.fromJsonObject(object);
            RequestTimeline timeline = new RequestTimeline(
                    extractCorrelationId(legacyEvent.getContext(), legacyEvent.getId()),
                    legacyEvent.getMethod(),
                    "legacy"
            );
            timeline.appendEvent(convertLegacyEvent(legacyEvent));
            timelines.add(timeline);
        }

        timelines.sort(new Comparator<RequestTimeline>() {
            @Override
            public int compare(RequestTimeline left, RequestTimeline right) {
                String leftUpdatedAt = left.getUpdatedAt() == null ? "" : left.getUpdatedAt();
                String rightUpdatedAt = right.getUpdatedAt() == null ? "" : right.getUpdatedAt();
                return rightUpdatedAt.compareTo(leftUpdatedAt);
            }
        });

        if (timelines.size() > maxTimelines) {
            timelines = new ArrayList<>(timelines.subList(0, maxTimelines));
        }

        return DiagnosticsSnapshot.create(timelines, timeSource.now());
    }

    private void writeSnapshot(DiagnosticsSnapshot snapshot) {
        storage.write(snapshot.toJsonString());
    }

    private TimelineEvent convertLegacyEvent(LogEvent event) {
        String correlationId = extractCorrelationId(event.getContext(), event.getId());
        int retryCount = extractRetryCount(event.getContext());
        Boolean retryable = isRetryableCode(event.getErrorCode());

        return TimelineEvent.stage(
                correlationId,
                event.getMethod(),
                "legacy",
                "legacy_log",
                hasText(event.getTimestamp()) ? event.getTimestamp() : timeSource.now(),
                retryCount,
                0L,
                null,
                retryable
        ).withCompletion(event.isResolved() ? "success" : "error", event.isResolved());
    }

    private String extractCorrelationId(String context, String fallback) {
        if (hasText(context)) {
            try {
                JSONObject object = new JSONObject(context);
                String correlationId = object.optString("correlationId", null);
                if (hasText(correlationId)) {
                    return correlationId;
                }
            } catch (Exception ignored) {
                int marker = context.indexOf("correlationId=");
                if (marker >= 0) {
                    String value = context.substring(marker + "correlationId=".length()).trim();
                    int separator = value.indexOf(',');
                    return separator >= 0 ? value.substring(0, separator).trim() : value;
                }
            }
        }
        return fallback;
    }

    private int extractRetryCount(String context) {
        if (!hasText(context)) {
            return 0;
        }

        try {
            JSONObject object = new JSONObject(context);
            if (object.has("totalAttempts")) {
                return Math.max(0, object.optInt("totalAttempts", 0));
            }
            if (object.has("attempt")) {
                return Math.max(0, object.optInt("attempt", 0));
            }
        } catch (Exception ignored) {
        }

        return 0;
    }

    private Boolean isRetryableCode(String errorCode) {
        if (!hasText(errorCode)) {
            return null;
        }
        return "1001".equals(errorCode)
                || "1002".equals(errorCode)
                || "9001".equals(errorCode);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    interface Storage {
        String read();

        void write(String value);

        void clear();
    }

    interface TimeSource {
        String now();

        static TimeSource system() {
            return new TimeSource() {
                @Override
                public String now() {
                    return Instant.now().toString();
                }
            };
        }
    }

    interface PlatformLogger {
        void error(String tag, String message, Throwable throwable);

        void debug(String tag, String message);

        void info(String tag, String message);

        static PlatformLogger android() {
            return new PlatformLogger() {
                @Override
                public void error(String tag, String message, Throwable throwable) {
                    if (throwable == null) {
                        Log.e(tag, message);
                    } else {
                        Log.e(tag, message, throwable);
                    }
                }

                @Override
                public void debug(String tag, String message) {
                    Log.d(tag, message);
                }

                @Override
                public void info(String tag, String message) {
                    Log.i(tag, message);
                }
            };
        }

        static PlatformLogger noOp() {
            return new PlatformLogger() {
                @Override
                public void error(String tag, String message, Throwable throwable) {
                }

                @Override
                public void debug(String tag, String message) {
                }

                @Override
                public void info(String tag, String message) {
                }
            };
        }
    }

    private static final class SharedPreferencesStorage implements Storage {
        private final SharedPreferences sharedPreferences;

        private SharedPreferencesStorage(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public String read() {
            return sharedPreferences.getString(KEY_LOGS, "");
        }

        @Override
        public void write(String value) {
            sharedPreferences.edit().putString(KEY_LOGS, value).apply();
        }

        @Override
        public void clear() {
            sharedPreferences.edit().remove(KEY_LOGS).apply();
        }
    }
}
