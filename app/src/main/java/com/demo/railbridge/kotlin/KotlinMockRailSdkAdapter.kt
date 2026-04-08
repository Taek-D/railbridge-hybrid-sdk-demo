package com.demo.railbridge.kotlin

import com.demo.railbridge.sdk.BalanceSnapshot
import com.demo.railbridge.sdk.ChargeResult
import com.demo.railbridge.sdk.MockRailSdk
import com.demo.railbridge.sdk.RailPlusSdkAdapter
import com.demo.railbridge.sdk.ScenarioController
import com.demo.railbridge.sdk.ScenarioOutcome
import com.demo.railbridge.sdk.SdkErrorCode
import com.demo.railbridge.sdk.SdkStatusSnapshot
import java.util.concurrent.ConcurrentHashMap

class KotlinMockRailSdkAdapter @JvmOverloads constructor(
    mockRailSdk: MockRailSdk,
    private val scenarioController: ScenarioController = ScenarioController()
) : RailPlusSdkAdapter {

    private val backend: Backend = LiveBackend(mockRailSdk)
    private val requestAttempts = ConcurrentHashMap<String, Int>()

    override fun initialize(callback: RailPlusSdkAdapter.Callback<Boolean>) {
        backend.initialize(callback)
    }

    override fun requestCharge(
        cardId: String,
        amount: Int,
        callback: RailPlusSdkAdapter.Callback<ChargeResult>
    ) {
        handleScenarioAwareRequest(
            buildRequestKey("requestCharge", cardId, amount),
            { wrapped -> backend.requestCharge(cardId, amount, wrapped) },
            callback
        )
    }

    override fun getBalance(cardId: String, callback: RailPlusSdkAdapter.Callback<BalanceSnapshot>) {
        handleScenarioAwareRequest(
            buildRequestKey("getBalance", cardId, null),
            { wrapped -> backend.getBalance(cardId, wrapped) },
            callback
        )
    }

    override fun getStatus(): SdkStatusSnapshot = backend.getStatus()

    override fun shutdown() {
        backend.shutdown()
    }

    fun getScenarioController(): ScenarioController = scenarioController

    private fun <T> handleScenarioAwareRequest(
        requestKey: String,
        backendRunner: (RailPlusSdkAdapter.Callback<T>) -> Unit,
        callback: RailPlusSdkAdapter.Callback<T>
    ) {
        val outcome = nextOutcome(requestKey)

        if (!outcome.emitsCallback()) {
            try {
                backendRunner(NoOpCallback())
            } finally {
                if (outcome.shouldClearAttemptsAfterHandling()) {
                    clearAttempts(requestKey)
                }
            }
            return
        }

        if (!outcome.usesBackendSuccessPath()) {
            try {
                callback.onError(outcome.errorCode)
            } finally {
                if (outcome.shouldClearAttemptsAfterHandling()) {
                    clearAttempts(requestKey)
                }
            }
            return
        }

        backendRunner(ScenarioCallback(callback, requestKey, outcome))
    }

    private fun nextOutcome(requestKey: String): ScenarioOutcome {
        val attempt = requestAttempts.merge(requestKey, 1) { left, right -> left + right } ?: 1
        return ScenarioOutcome.forAttempt(scenarioController.activePreset, attempt)
    }

    private fun clearAttempts(requestKey: String) {
        requestAttempts.remove(requestKey)
    }

    private fun buildRequestKey(method: String, cardId: String?, amount: Int?): String {
        return buildString {
            append(method)
            append('|')
            append(cardId?.trim().orEmpty())
            if (amount != null) {
                append('|')
                append(amount)
            }
        }
    }

    interface Backend {
        fun initialize(callback: RailPlusSdkAdapter.Callback<Boolean>)
        fun requestCharge(cardId: String, amount: Int, callback: RailPlusSdkAdapter.Callback<ChargeResult>)
        fun getBalance(cardId: String, callback: RailPlusSdkAdapter.Callback<BalanceSnapshot>)
        fun getStatus(): SdkStatusSnapshot
        fun shutdown()
    }

    private inner class ScenarioCallback<T>(
        private val callback: RailPlusSdkAdapter.Callback<T>,
        private val requestKey: String,
        private val outcome: ScenarioOutcome
    ) : RailPlusSdkAdapter.Callback<T> {
        override fun onSuccess(result: T) {
            try {
                callback.onSuccess(result)
                if (outcome.isDuplicateCallback()) {
                    callback.onSuccess(result)
                }
            } finally {
                clearAttempts(requestKey)
            }
        }

        override fun onError(errorCode: SdkErrorCode) {
            try {
                callback.onError(errorCode)
            } finally {
                clearAttempts(requestKey)
            }
        }
    }

    private class NoOpCallback<T> : RailPlusSdkAdapter.Callback<T> {
        override fun onSuccess(result: T) = Unit
        override fun onError(errorCode: SdkErrorCode) = Unit
    }

    private class LiveBackend(private val mockRailSdk: MockRailSdk) : Backend {
        override fun initialize(callback: RailPlusSdkAdapter.Callback<Boolean>) {
            mockRailSdk.initialize(object : MockRailSdk.SdkCallback<Boolean> {
                override fun onSuccess(result: Boolean) = callback.onSuccess(result)
                override fun onError(errorCode: SdkErrorCode) = callback.onError(errorCode)
            })
        }

        override fun requestCharge(
            cardId: String,
            amount: Int,
            callback: RailPlusSdkAdapter.Callback<ChargeResult>
        ) {
            mockRailSdk.charge(cardId, amount, object : MockRailSdk.SdkCallback<ChargeResult> {
                override fun onSuccess(result: ChargeResult) = callback.onSuccess(result)
                override fun onError(errorCode: SdkErrorCode) = callback.onError(errorCode)
            })
        }

        override fun getBalance(cardId: String, callback: RailPlusSdkAdapter.Callback<BalanceSnapshot>) {
            mockRailSdk.getBalance(cardId, object : MockRailSdk.SdkCallback<MockRailSdk.BalanceResult> {
                override fun onSuccess(result: MockRailSdk.BalanceResult) {
                    callback.onSuccess(
                        BalanceSnapshot(
                            result.cardId,
                            result.balance,
                            result.timestamp
                        )
                    )
                }

                override fun onError(errorCode: SdkErrorCode) = callback.onError(errorCode)
            })
        }

        override fun getStatus(): SdkStatusSnapshot {
            return SdkStatusSnapshot(mockRailSdk.isInitialized, mockRailSdk.sdkVersion)
        }

        override fun shutdown() {
            mockRailSdk.shutdown()
        }
    }
}
