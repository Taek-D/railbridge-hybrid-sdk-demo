package com.demo.railbridge.sdk;

import android.os.Handler;
import android.os.Looper;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MockRailSdk {

    private static final String SDK_VERSION = "1.2.3-mock";
    private static final int DEFAULT_BALANCE = 38947;
    private static final int CHARGE_DELAY_MS = 180;
    private static final int BALANCE_DELAY_MS = 120;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean initialized;

    public interface SdkCallback<T> {
        void onSuccess(T result);

        void onError(SdkErrorCode errorCode);
    }

    public void initialize(SdkCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Thread.sleep(100L);
                initialized = true;
                postSuccess(callback, true);
            } catch (Exception e) {
                postError(callback, SdkErrorCode.ERR_SDK_INTERNAL);
            }
        });
    }

    public void charge(String cardId, int amount, SdkCallback<ChargeResult> callback) {
        if (!initialized) {
            postError(callback, SdkErrorCode.ERR_SDK_INTERNAL);
            return;
        }
        if (!validateCardId(cardId)) {
            postError(callback, SdkErrorCode.ERR_INVALID_CARD);
            return;
        }
        if (amount <= 0) {
            postError(callback, SdkErrorCode.ERR_UNKNOWN);
            return;
        }

        executor.execute(() -> {
            try {
                Thread.sleep(CHARGE_DELAY_MS);
                String transactionId = "TXN_" + System.currentTimeMillis();
                int newBalance = DEFAULT_BALANCE + amount;
                postSuccess(callback, new ChargeResult(transactionId, amount, newBalance, Instant.now().toString()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                postError(callback, SdkErrorCode.ERR_TIMEOUT);
            } catch (Exception e) {
                postError(callback, SdkErrorCode.ERR_UNKNOWN);
            }
        });
    }

    public void getBalance(String cardId, SdkCallback<BalanceResult> callback) {
        if (!initialized) {
            postError(callback, SdkErrorCode.ERR_SDK_INTERNAL);
            return;
        }
        if (!validateCardId(cardId)) {
            postError(callback, SdkErrorCode.ERR_INVALID_CARD);
            return;
        }

        executor.execute(() -> {
            try {
                Thread.sleep(BALANCE_DELAY_MS);
                postSuccess(callback, new BalanceResult(cardId, DEFAULT_BALANCE, Instant.now().toString()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                postError(callback, SdkErrorCode.ERR_TIMEOUT);
            } catch (Exception e) {
                postError(callback, SdkErrorCode.ERR_UNKNOWN);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }

    public String getSdkVersion() {
        return SDK_VERSION;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private boolean validateCardId(String cardId) {
        return cardId != null && !cardId.trim().isEmpty() && cardId.startsWith("CARD_");
    }

    private <T> void postSuccess(SdkCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postError(SdkCallback<T> callback, SdkErrorCode errorCode) {
        mainHandler.post(() -> callback.onError(errorCode));
    }

    public static class BalanceResult {
        private final String cardId;
        private final int balance;
        private final String timestamp;

        public BalanceResult(String cardId, int balance, String timestamp) {
            this.cardId = cardId;
            this.balance = balance;
            this.timestamp = timestamp;
        }

        public String getCardId() {
            return cardId;
        }

        public int getBalance() {
            return balance;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }
}
