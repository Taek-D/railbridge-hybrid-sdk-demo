package com.demo.railbridge.sdk;

public class MockRailSdkAdapter implements RailPlusSdkAdapter {

    private final Backend backend;

    public MockRailSdkAdapter(MockRailSdk mockRailSdk) {
        this(new LiveBackend(mockRailSdk));
    }

    MockRailSdkAdapter(Backend backend) {
        this.backend = backend;
    }

    @Override
    public void initialize(Callback<Boolean> callback) {
        backend.initialize(callback);
    }

    @Override
    public void requestCharge(String cardId, int amount, Callback<ChargeResult> callback) {
        backend.requestCharge(cardId, amount, callback);
    }

    @Override
    public void getBalance(String cardId, Callback<BalanceSnapshot> callback) {
        backend.getBalance(cardId, callback);
    }

    @Override
    public SdkStatusSnapshot getStatus() {
        return backend.getStatus();
    }

    @Override
    public void shutdown() {
        backend.shutdown();
    }

    interface Backend {
        void initialize(Callback<Boolean> callback);

        void requestCharge(String cardId, int amount, Callback<ChargeResult> callback);

        void getBalance(String cardId, Callback<BalanceSnapshot> callback);

        SdkStatusSnapshot getStatus();

        void shutdown();
    }

    private static final class LiveBackend implements Backend {

        private final MockRailSdk mockRailSdk;

        private LiveBackend(MockRailSdk mockRailSdk) {
            this.mockRailSdk = mockRailSdk;
        }

        @Override
        public void initialize(Callback<Boolean> callback) {
            mockRailSdk.initialize(new MockRailSdk.SdkCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onError(SdkErrorCode errorCode) {
                    callback.onError(errorCode);
                }
            });
        }

        @Override
        public void requestCharge(String cardId, int amount, Callback<ChargeResult> callback) {
            mockRailSdk.charge(cardId, amount, new MockRailSdk.SdkCallback<ChargeResult>() {
                @Override
                public void onSuccess(ChargeResult result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onError(SdkErrorCode errorCode) {
                    callback.onError(errorCode);
                }
            });
        }

        @Override
        public void getBalance(String cardId, Callback<BalanceSnapshot> callback) {
            mockRailSdk.getBalance(cardId, new MockRailSdk.SdkCallback<MockRailSdk.BalanceResult>() {
                @Override
                public void onSuccess(MockRailSdk.BalanceResult result) {
                    callback.onSuccess(new BalanceSnapshot(
                            result.getCardId(),
                            result.getBalance(),
                            result.getTimestamp()
                    ));
                }

                @Override
                public void onError(SdkErrorCode errorCode) {
                    callback.onError(errorCode);
                }
            });
        }

        @Override
        public SdkStatusSnapshot getStatus() {
            return new SdkStatusSnapshot(mockRailSdk.isInitialized(), mockRailSdk.getSdkVersion());
        }

        @Override
        public void shutdown() {
            mockRailSdk.shutdown();
        }
    }
}
