package com.demo.railbridge.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.demo.railbridge.sdk.SdkErrorCode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ErrorLogger {

    private static final String TAG = "RailBridge.ErrorLogger";
    private static final String PREFS_NAME = "railbridge_error_logs";
    private static final String KEY_LOGS = "error_logs";
    private static final int MAX_LOGS = 50;

    private static ErrorLogger instance;

    private final SharedPreferences sharedPreferences;
    private boolean crashlyticsEnabled;

    private ErrorLogger(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
        Log.e(TAG, event.toString());

        if (crashlyticsEnabled) {
            Log.d(TAG, "[Crashlytics] Logged: " + event.getId());
        }

        saveLog(event);
    }

    public void logSdkFailure(String method, SdkErrorCode code, String context) {
        if (crashlyticsEnabled) {
            Log.d(TAG, "[Crashlytics] Custom keys set: code=" + code.getCode() + ", method=" + method);
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

    public synchronized List<LogEvent> getRecentLogs() {
        return parseLogs(sharedPreferences.getString(KEY_LOGS, "[]"));
    }

    public void clearLogs() {
        sharedPreferences.edit().remove(KEY_LOGS).apply();
        Log.i(TAG, "Logs cleared");
    }

    private void saveLog(LogEvent event) {
        List<LogEvent> logs = getRecentLogs();
        logs.add(0, event);

        if (logs.size() > MAX_LOGS) {
            logs = new ArrayList<>(logs.subList(0, MAX_LOGS));
        }

        JSONArray array = new JSONArray();
        for (LogEvent log : logs) {
            array.put(log.toJsonObject());
        }
        sharedPreferences.edit().putString(KEY_LOGS, array.toString()).apply();
    }

    private List<LogEvent> parseLogs(String json) {
        List<LogEvent> logs = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return logs;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    logs.add(LogEvent.fromJsonObject(object));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse logs", e);
        }

        return logs;
    }
}
