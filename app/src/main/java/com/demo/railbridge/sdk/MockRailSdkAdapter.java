package com.demo.railbridge.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockRailSdkAdapter implements RailPlusSdkAdapter {

    private final Backend backend;
    private final ScenarioController scenarioController;
    private final Map<String, Integer> requestAttempts = new ConcurrentHashMap<>();

    public MockRailSdkAdapter(MockRailSdk mockRailSdk) {
        this(new LiveBackend(mockRailSdk), new ScenarioController());
    }

    public MockRailSdkAdapter(MockRailSdk mockRailSdk, ScenarioController scenarioController) {
        this(new LiveBackend(mockRailSdk), scenarioController);
    }

    MockRailSdkAdapter(Backend backend) {
        this(backend, new ScenarioController());
    }

    MockRailSdkAdapter(Backend backend, ScenarioController scenarioController) {
        this.backend = backend;
        this.scenarioController = scenarioController == null ? new ScenarioController() : scenarioController;
    }

    @Override
    public void initialize(Callback<Boolean> callback) {
        backend.initialize(callback);
    }

    @Override
    public void requestCharge(String cardId, int amount, Callback<ChargeResult> callback) {
        handleScenarioAwareRequest(
                buildRequestKey("requestCharge", cardId, amount),
                wrappedCallback -> backend.requestCharge(cardId, amount, wrappedCallback),
                callback
        );
    }

    @Override
    public void getBalance(String cardId, Callback<BalanceSnapshot> callback) {
        handleScenarioAwareRequest(
                buildRequestKey("getBalance", cardId, null),
                wrappedCallback -> backend.getBalance(cardId, wrappedCallback),
                callback
        );
    }

    @Override
    public SdkStatusSnapshot getStatus() {
        return backend.getStatus();
    }

    @Override
    public void shutdown() {
        backend.shutdown();
    }

    public ScenarioController getScenarioController() {
        return scenarioController;
    }

    private <T> void handleScenarioAwareRequest(
            String requestKey,
            BackendRunner<T> backendRunner,
            Callback<T> callback
    ) {
        ScenarioOutcome outcome = nextOutcome(requestKey);

        if (!outcome.emitsCallback()) {
            try {
                backendRunner.run(new NoOpCallback<T>());
            } finally {
                if (outcome.shouldClearAttemptsAfterHandling()) {
                    clearAttempts(requestKey);
                }
            }
            return;
        }

        if (!outcome.usesBackendSuccessPath()) {
            try {
                callback.onError(outcome.getErrorCode());
            } finally {
                if (outcome.shouldClearAttemptsAfterHandling()) {
                    clearAttempts(requestKey);
                }
            }
            return;
        }

        backendRunner.run(new ScenarioCallback<>(callback, requestKey, outcome));
    }

    private ScenarioOutcome nextOutcome(String requestKey) {
        int attempt = requestAttempts.merge(requestKey, 1, Integer::sum);
        return ScenarioOutcome.forAttempt(scenarioController.getActivePreset(), attempt);
    }

    private void clearAttempts(String requestKey) {
        requestAttempts.remove(requestKey);
    }

    private String buildRequestKey(String method, String cardId, Integer amount) {
        StringBuilder builder = new StringBuilder(method)
                .append('|')
                .append(cardId == null ? "" : cardId.trim());
        if (amount != null) {
            builder.append('|').append(amount);
        }
        return builder.toString();
    }

    interface Backend {
        void initialize(Callback<Boolean> callback);

        void requestCharge(String cardId, int amount, Callback<ChargeResult> callback);

        void getBalance(String cardId, Callback<BalanceSnapshot> callback);

        SdkStatusSnapshot getStatus();

        void shutdown();
    }

    private interface BackendRunner<T> {
        void run(Callback<T> callback);
    }

    private final class ScenarioCallback<T> implements Callback<T> {
        private final Callback<T> callback;
        private final String requestKey;
        private final ScenarioOutcome outcome;

        private ScenarioCallback(Callback<T> callback, String requestKey, ScenarioOutcome outcome) {
            this.callback = callback;
            this.requestKey = requestKey;
            this.outcome = outcome;
        }

        @Override
        public void onSuccess(T result) {
            try {
                callback.onSuccess(result);
                if (outcome.isDuplicateCallback()) {
                    callback.onSuccess(result);
                }
            } finally {
                clearAttempts(requestKey);
            }
        }

        @Override
        public void onError(SdkErrorCode errorCode) {
            try {
                callback.onError(errorCode);
            } finally {
                clearAttempts(requestKey);
            }
        }
    }

    private static final class NoOpCallback<T> implements Callback<T> {
        @Override
        public void onSuccess(T result) {
        }

        @Override
        public void onError(SdkErrorCode errorCode) {
        }
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
