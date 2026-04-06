package com.demo.railbridge.sdk;

import android.os.Handler;
import android.os.Looper;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MockRailSdk {

    private static final String SDK_VERSION = "1.2.3-mock";
    private static final int TIMEOUT_MS = 5000;

    private final Random random = new Random();
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
                int delay = 200 + random.nextInt(301);
                Thread.sleep(delay);

                if (delay > TIMEOUT_MS) {
                    postError(callback, SdkErrorCode.ERR_TIMEOUT);
                    return;
                }

                int roll = random.nextInt(100);
                if (roll < 80) {
                    String transactionId = "TXN_" + System.currentTimeMillis() + "_" + String.format("%03d", random.nextInt(1000));
                    int currentBalance = 35000 + random.nextInt(20000);
                    int newBalance = currentBalance + amount;
                    postSuccess(callback, new ChargeResult(transactionId, amount, newBalance, Instant.now().toString()));
                } else if (roll < 90) {
                    postError(callback, SdkErrorCode.ERR_SDK_INTERNAL);
                } else if (roll < 95) {
                    postError(callback, SdkErrorCode.ERR_NETWORK_TIMEOUT);
                } else {
                    postError(callback, SdkErrorCode.ERR_INSUFFICIENT_BALANCE);
                }
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
                int delay = 100 + random.nextInt(201);
                Thread.sleep(delay);

                int roll = random.nextInt(100);
                if (roll < 90) {
                    int balance = 20000 + random.nextInt(50000);
                    postSuccess(callback, new BalanceResult(cardId, balance, Instant.now().toString()));
                } else if (roll < 95) {
                    postError(callback, SdkErrorCode.ERR_SDK_INTERNAL);
                } else {
                    postError(callback, SdkErrorCode.ERR_NETWORK_TIMEOUT);
                }
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
