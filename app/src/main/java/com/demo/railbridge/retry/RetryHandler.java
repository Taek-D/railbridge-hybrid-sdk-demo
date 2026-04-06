package com.demo.railbridge.retry;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.demo.railbridge.logging.ErrorLogger;
import com.demo.railbridge.sdk.SdkErrorCode;

import org.json.JSONObject;

public class RetryHandler {

    private static final String TAG = "RailBridge.RetryHandler";
    private static final long[] BACKOFF_DELAYS = {500L, 1000L, 2000L};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ErrorLogger errorLogger;

    public RetryHandler(ErrorLogger errorLogger) {
        this.errorLogger = errorLogger;
    }

    public static boolean isRetryable(SdkErrorCode errorCode) {
        return errorCode == SdkErrorCode.ERR_NETWORK_TIMEOUT
                || errorCode == SdkErrorCode.ERR_SDK_INTERNAL;
    }

    public static int getMaxRetry() {
        return BACKOFF_DELAYS.length;
    }

    public static long getBackoffDelay(int retryNumber) {
        int index = Math.max(0, Math.min(retryNumber - 1, BACKOFF_DELAYS.length - 1));
        return BACKOFF_DELAYS[index];
    }

    public void execute(RetryTask task, RetryCallback callback) {
        execute(task, 1, callback);
    }

    public void cancel() {
        handler.removeCallbacksAndMessages(null);
    }

    private void execute(RetryTask task, int attempt, RetryCallback callback) {
        Log.d(TAG, "Executing attempt: " + attempt);

        task.run(resultJson -> {
            Log.d(TAG, "Task succeeded on attempt: " + attempt);
            int retryCount = Math.max(0, attempt - 1);
            if (retryCount > 0) {
                errorLogger.logRetrySuccess(task.getMethodName(), retryCount);
            }
            callback.onSuccess(addRetryCount(resultJson, retryCount));
        }, errorCode -> {
            Log.w(TAG, "Task failed on attempt " + attempt + ": " + errorCode);
            errorLogger.logRetryAttempt(task.getMethodName(), attempt, errorCode);

            if (isRetryable(errorCode) && attempt <= getMaxRetry()) {
                int retryNumber = attempt;
                long delay = getBackoffDelay(retryNumber);
                Log.d(TAG, "Retrying after " + delay + "ms (attempt " + (attempt + 1) + ")");
                handler.postDelayed(() -> execute(task, attempt + 1, callback), delay);
                return;
            }

            int retryCount = Math.min(Math.max(0, attempt - 1), getMaxRetry());
            SdkErrorCode finalError = isRetryable(errorCode) && retryCount >= getMaxRetry()
                    ? SdkErrorCode.RETRY_EXHAUSTED
                    : errorCode;

            if (finalError == SdkErrorCode.RETRY_EXHAUSTED) {
                Log.e(TAG, "Retry exhausted after " + attempt + " attempts");
            } else {
                Log.e(TAG, "Non-retryable error: " + errorCode);
            }

            callback.onError(finalError, retryCount);
        });
    }

    private String addRetryCount(String resultJson, int retryCount) {
        try {
            JSONObject object = new JSONObject(resultJson);
            object.put("retryCount", retryCount);
            return object.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to append retryCount", e);
            return resultJson;
        }
    }

    public interface RetryTask {
        void run(OnSuccessCallback onSuccess, OnErrorCallback onError);

        String getMethodName();
    }

    public interface OnSuccessCallback {
        void onResult(String resultJson);
    }

    public interface OnErrorCallback {
        void onError(SdkErrorCode errorCode);
    }

    public interface RetryCallback {
        void onSuccess(String resultJson);

        void onError(SdkErrorCode errorCode, int retryCount);
    }
}
