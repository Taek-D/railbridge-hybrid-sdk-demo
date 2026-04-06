package com.demo.railbridge.bridge;

public interface BridgeCallback {
    void onSuccess(String resultJson);

    void onError(String errorJson);
}
