package com.demo.railbridge.sdk;

public enum SdkErrorCode {
    ERR_SDK_INTERNAL("1001", "SDK internal error"),
    ERR_NETWORK_TIMEOUT("1002", "Network timeout"),
    ERR_INSUFFICIENT_BALANCE("1003", "Insufficient balance"),
    ERR_INVALID_CARD("1004", "Invalid card"),
    ERR_TIMEOUT("1005", "Request timeout"),
    ERR_VENDOR_INTERNAL("1006", "Vendor internal error"),
    ERR_UNKNOWN("9999", "Unknown error"),
    RETRY_EXHAUSTED("9001", "Retry attempts exhausted");

    private final String code;
    private final String message;

    SdkErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
