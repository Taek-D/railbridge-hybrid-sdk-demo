package com.demo.railbridge.sdk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MockRailSdkAdapterTest {

    @Test
    public void initialize_forwardsSuccessWithoutTransformingValue() {
        FakeBackend backend = new FakeBackend();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend);
        TestCallback<Boolean> callback = new TestCallback<>();

        adapter.initialize(callback);

        assertTrue(callback.successCalled);
        assertEquals(Boolean.TRUE, callback.successValue);
        assertFalse(callback.errorCalled);
    }

    @Test
    public void requestCharge_forwardsOriginalChargeResult() {
        FakeBackend backend = new FakeBackend();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend);
        TestCallback<ChargeResult> callback = new TestCallback<>();

        adapter.requestCharge("CARD_001", 10000, callback);

        assertTrue(callback.successCalled);
        assertSame(backend.chargeResult, callback.successValue);
        assertEquals("CARD_001", backend.lastCardId);
        assertEquals(10000, backend.lastAmount);
    }

    @Test
    public void getBalance_returnsSdkNeutralSnapshot() {
        FakeBackend backend = new FakeBackend();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend);
        TestCallback<BalanceSnapshot> callback = new TestCallback<>();

        adapter.getBalance("CARD_001", callback);

        assertTrue(callback.successCalled);
        assertEquals("CARD_001", callback.successValue.getCardId());
        assertEquals(38947, callback.successValue.getBalance());
        assertEquals("2026-04-06T11:01:37Z", callback.successValue.getTimestamp());
    }

    @Test
    public void getStatus_mapsInitializedStateAndVersion() {
        FakeBackend backend = new FakeBackend();
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend);

        SdkStatusSnapshot status = adapter.getStatus();

        assertTrue(status.isInitialized());
        assertEquals("1.2.3-mock", status.getVersion());
    }

    @Test
    public void adapter_preservesSdkErrors() {
        FakeBackend backend = new FakeBackend();
        backend.errorCode = SdkErrorCode.ERR_NETWORK_TIMEOUT;
        backend.failCharge = true;
        MockRailSdkAdapter adapter = new MockRailSdkAdapter(backend);
        TestCallback<ChargeResult> callback = new TestCallback<>();

        adapter.requestCharge("CARD_001", 10000, callback);

        assertTrue(callback.errorCalled);
        assertEquals(SdkErrorCode.ERR_NETWORK_TIMEOUT, callback.errorValue);
        assertFalse(callback.successCalled);
    }

    private static final class FakeBackend implements MockRailSdkAdapter.Backend {
        private final ChargeResult chargeResult =
                new ChargeResult("TXN_123", 10000, 45000, "2026-04-06T11:00:00Z");
        private final BalanceSnapshot balanceSnapshot =
                new BalanceSnapshot("CARD_001", 38947, "2026-04-06T11:01:37Z");

        private String lastCardId;
        private int lastAmount;
        private boolean failCharge;
        private SdkErrorCode errorCode = SdkErrorCode.ERR_UNKNOWN;

        @Override
        public void initialize(RailPlusSdkAdapter.Callback<Boolean> callback) {
            callback.onSuccess(true);
        }

        @Override
        public void requestCharge(String cardId, int amount, RailPlusSdkAdapter.Callback<ChargeResult> callback) {
            lastCardId = cardId;
            lastAmount = amount;
            if (failCharge) {
                callback.onError(errorCode);
                return;
            }
            callback.onSuccess(chargeResult);
        }

        @Override
        public void getBalance(String cardId, RailPlusSdkAdapter.Callback<BalanceSnapshot> callback) {
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

    private static final class TestCallback<T> implements RailPlusSdkAdapter.Callback<T> {
        private boolean successCalled;
        private boolean errorCalled;
        private T successValue;
        private SdkErrorCode errorValue;

        @Override
        public void onSuccess(T result) {
            successCalled = true;
            successValue = result;
        }

        @Override
        public void onError(SdkErrorCode errorCode) {
            errorCalled = true;
            errorValue = errorCode;
        }
    }
}
