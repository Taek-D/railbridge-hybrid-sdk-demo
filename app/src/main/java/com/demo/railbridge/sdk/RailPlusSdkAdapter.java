package com.demo.railbridge.sdk;

public interface RailPlusSdkAdapter {

    void initialize(Callback<Boolean> callback);

    void requestCharge(String cardId, int amount, Callback<ChargeResult> callback);

    void getBalance(String cardId, Callback<BalanceSnapshot> callback);

    SdkStatusSnapshot getStatus();

    void shutdown();

    interface Callback<T> {
        void onSuccess(T result);

        void onError(SdkErrorCode errorCode);
    }
}
