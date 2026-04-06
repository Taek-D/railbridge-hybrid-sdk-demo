package com.demo.railbridge.sdk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class MockRailSdkScenarioTest {

    @Test
    public void scenarioController_defaultsToNormalAndParsesPresetValues() {
        ScenarioController controller = new ScenarioController();

        assertEquals(ScenarioPreset.NORMAL, controller.getActivePreset());

        controller.setActivePreset(ScenarioPreset.fromValue("duplicate_callback"));

        assertEquals(ScenarioPreset.DUPLICATE_CALLBACK, controller.getActivePreset());
    }

    @Test
    public void timeoutScenario_failsOnceThenRecoversOnLaterAttempt() {
        FakeBackend backend = new FakeBackend();
        ScenarioController controller = new ScenarioController();
        controller.setActivePreset(ScenarioPreset.TIMEOUT);
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend, controller);
        RecordingCallback<ChargeResult> firstCallback = new RecordingCallback<>();

        adapter.requestCharge("CARD_001", 10000, firstCallback);

        assertEquals(1, firstCallback.errorCount);
        assertEquals(SdkErrorCode.ERR_NETWORK_TIMEOUT, firstCallback.lastError);
        assertEquals(0, backend.chargeCallCount);

        RecordingCallback<ChargeResult> secondCallback = new RecordingCallback<>();
        adapter.requestCharge("CARD_001", 10000, secondCallback);

        assertEquals(1, secondCallback.successCount);
        assertEquals("TXN_123", secondCallback.lastSuccess.getTransactionId());
        assertEquals(1, backend.chargeCallCount);
    }

    @Test
    public void internalErrorScenario_staysDistinctFromRetryExhausted() {
        ScenarioOutcome outcome = ScenarioOutcome.forAttempt(ScenarioPreset.INTERNAL_ERROR, 1);
        RecordingCallback<ChargeResult> callback = new RecordingCallback<>();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(new FakeBackend(), new ScenarioController(ScenarioPreset.INTERNAL_ERROR));

        adapter.requestCharge("CARD_001", 10000, callback);

        assertEquals(SdkErrorCode.ERR_VENDOR_INTERNAL, outcome.getErrorCode());
        assertFalse(outcome.isRetryable());
        assertEquals("VENDOR_INTERNAL", outcome.getVendorCode());
        assertNotEquals(SdkErrorCode.RETRY_EXHAUSTED, outcome.getErrorCode());
        assertEquals(1, callback.errorCount);
        assertEquals(SdkErrorCode.ERR_VENDOR_INTERNAL, callback.lastError);
    }

    @Test
    public void callbackLossScenario_startsBackendWorkWithoutEmittingCallback() {
        FakeBackend backend = new FakeBackend();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend, new ScenarioController(ScenarioPreset.CALLBACK_LOSS));
        RecordingCallback<BalanceSnapshot> callback = new RecordingCallback<>();

        adapter.getBalance("CARD_001", callback);

        assertEquals(1, backend.balanceCallCount);
        assertEquals(0, callback.successCount);
        assertEquals(0, callback.errorCount);
    }

    @Test
    public void duplicateCallbackScenario_emitsSameSuccessTwice() {
        FakeBackend backend = new FakeBackend();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend, new ScenarioController(ScenarioPreset.DUPLICATE_CALLBACK));
        RecordingCallback<ChargeResult> callback = new RecordingCallback<>();

        adapter.requestCharge("CARD_001", 10000, callback);

        assertEquals(1, backend.chargeCallCount);
        assertEquals(2, callback.successCount);
        assertEquals("TXN_123", callback.lastSuccess.getTransactionId());
    }

    @Test
    public void retryExhaustedScenario_keepsReturningRetryableFailuresForTheBridgeLoop() {
        ScenarioOutcome outcome = ScenarioOutcome.forAttempt(ScenarioPreset.RETRY_EXHAUSTED, 3);
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(new FakeBackend(), new ScenarioController(ScenarioPreset.RETRY_EXHAUSTED));
        RecordingCallback<ChargeResult> firstCallback = new RecordingCallback<>();
        RecordingCallback<ChargeResult> secondCallback = new RecordingCallback<>();

        adapter.requestCharge("CARD_001", 10000, firstCallback);
        adapter.requestCharge("CARD_001", 10000, secondCallback);

        assertEquals(SdkErrorCode.ERR_NETWORK_TIMEOUT, outcome.getErrorCode());
        assertTrue(outcome.isRetryable());
        assertEquals("VENDOR_TIMEOUT", outcome.getVendorCode());
        assertEquals(1, firstCallback.errorCount);
        assertEquals(1, secondCallback.errorCount);
        assertEquals(SdkErrorCode.ERR_NETWORK_TIMEOUT, firstCallback.lastError);
        assertEquals(SdkErrorCode.ERR_NETWORK_TIMEOUT, secondCallback.lastError);
    }

    private static final class FakeBackend implements MockRailSdkAdapter.Backend {
        private final ChargeResult chargeResult =
                new ChargeResult("TXN_123", 10000, 45000, "2026-04-06T11:00:00Z");
        private final BalanceSnapshot balanceSnapshot =
                new BalanceSnapshot("CARD_001", 38947, "2026-04-06T11:01:37Z");

        private int chargeCallCount;
        private int balanceCallCount;

        @Override
        public void initialize(RailPlusSdkAdapter.Callback<Boolean> callback) {
            callback.onSuccess(true);
        }

        @Override
        public void requestCharge(String cardId, int amount, RailPlusSdkAdapter.Callback<ChargeResult> callback) {
            chargeCallCount++;
            callback.onSuccess(chargeResult);
        }

        @Override
        public void getBalance(String cardId, RailPlusSdkAdapter.Callback<BalanceSnapshot> callback) {
            balanceCallCount++;
            callback.onSuccess(balanceSnapshot);
        }

        @Override
        public SdkStatusSnapshot getStatus() {
            return new SdkStatusSnapshot(true, "1.2.3-mock");
        }

        @Override
        public void shutdown() {
        }
    }

    private static final class RecordingCallback<T> implements RailPlusSdkAdapter.Callback<T> {
        private int successCount;
        private int errorCount;
        private T lastSuccess;
        private SdkErrorCode lastError;

        @Override
        public void onSuccess(T result) {
            successCount++;
            lastSuccess = result;
        }

        @Override
        public void onError(SdkErrorCode errorCode) {
            errorCount++;
            lastError = errorCode;
        }
    }
}
