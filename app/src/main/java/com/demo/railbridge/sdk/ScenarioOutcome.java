package com.demo.railbridge.sdk;

public final class ScenarioOutcome {

    private final boolean emitCallback;
    private final boolean useBackendSuccessPath;
    private final boolean duplicateCallback;
    private final boolean clearAttemptsAfterHandling;
    private final SdkErrorCode errorCode;
    private final String vendorCode;
    private final boolean retryable;

    private ScenarioOutcome(
            boolean emitCallback,
            boolean useBackendSuccessPath,
            boolean duplicateCallback,
            boolean clearAttemptsAfterHandling,
            SdkErrorCode errorCode,
            String vendorCode,
            boolean retryable
    ) {
        this.emitCallback = emitCallback;
        this.useBackendSuccessPath = useBackendSuccessPath;
        this.duplicateCallback = duplicateCallback;
        this.clearAttemptsAfterHandling = clearAttemptsAfterHandling;
        this.errorCode = errorCode;
        this.vendorCode = vendorCode;
        this.retryable = retryable;
    }

    public static ScenarioOutcome forAttempt(ScenarioPreset preset, int attempt) {
        ScenarioPreset safePreset = preset == null ? ScenarioPreset.NORMAL : preset;
        int safeAttempt = Math.max(1, attempt);

        switch (safePreset) {
            case TIMEOUT:
                if (safeAttempt == 1) {
                    return failure(SdkErrorCode.ERR_NETWORK_TIMEOUT, "VENDOR_TIMEOUT", true, false);
                }
                return backendSuccess(false);
            case INTERNAL_ERROR:
                return failure(SdkErrorCode.ERR_VENDOR_INTERNAL, "VENDOR_INTERNAL", false, true);
            case CALLBACK_LOSS:
                return callbackLoss();
            case DUPLICATE_CALLBACK:
                return backendSuccess(true);
            case RETRY_EXHAUSTED:
                return failure(SdkErrorCode.ERR_NETWORK_TIMEOUT, "VENDOR_TIMEOUT", true, true);
            case NORMAL:
            default:
                return backendSuccess(false);
        }
    }

    private static ScenarioOutcome backendSuccess(boolean duplicateCallback) {
        return new ScenarioOutcome(true, true, duplicateCallback, true, null, null, false);
    }

    private static ScenarioOutcome failure(
            SdkErrorCode errorCode,
            String vendorCode,
            boolean retryable,
            boolean clearAttemptsAfterHandling
    ) {
        return new ScenarioOutcome(
                true,
                false,
                false,
                clearAttemptsAfterHandling,
                errorCode,
                vendorCode,
                retryable
        );
    }

    private static ScenarioOutcome callbackLoss() {
        return new ScenarioOutcome(false, false, false, true, null, "CALLBACK_LOSS", false);
    }

    public boolean emitsCallback() {
        return emitCallback;
    }

    public boolean usesBackendSuccessPath() {
        return useBackendSuccessPath;
    }

    public boolean isDuplicateCallback() {
        return duplicateCallback;
    }

    public boolean shouldClearAttemptsAfterHandling() {
        return clearAttemptsAfterHandling;
    }

    public SdkErrorCode getErrorCode() {
        return errorCode;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
