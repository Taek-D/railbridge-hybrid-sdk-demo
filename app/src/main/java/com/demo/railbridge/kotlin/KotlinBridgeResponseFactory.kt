package com.demo.railbridge.kotlin

import com.demo.railbridge.sdk.BalanceSnapshot
import com.demo.railbridge.sdk.ChargeResult
import com.demo.railbridge.sdk.SdkErrorCode
import com.demo.railbridge.sdk.SdkStatusSnapshot
import org.json.JSONException
import org.json.JSONObject

object KotlinBridgeResponseFactory {

    @JvmStatic
    fun buildChargeSuccess(result: ChargeResult, retryCount: Int, metadata: Metadata?): String {
        return try {
            val data = JSONObject()
                .put("transactionId", result.transactionId)
                .put("amount", result.amount)
                .put("balance", result.balance)
                .put("timestamp", result.timestamp)
            buildSuccess("requestCharge", data, retryCount, metadata)
        } catch (_: JSONException) {
            buildError("requestCharge", SdkErrorCode.ERR_UNKNOWN, retryCount, null, null, null, metadata)
        }
    }

    @JvmStatic
    fun buildBalanceSuccess(result: BalanceSnapshot, retryCount: Int, metadata: Metadata?): String {
        return try {
            val data = JSONObject()
                .put("cardId", result.cardId)
                .put("balance", result.balance)
                .put("timestamp", result.timestamp)
            buildSuccess("getBalance", data, retryCount, metadata)
        } catch (_: JSONException) {
            buildError("getBalance", SdkErrorCode.ERR_UNKNOWN, retryCount, null, null, null, metadata)
        }
    }

    @JvmStatic
    fun buildStatusSuccess(result: SdkStatusSnapshot, metadata: Metadata?): String {
        return try {
            val data = JSONObject()
                .put("initialized", result.isInitialized)
                .put("version", result.version)
            buildSuccess("getSdkStatus", data, 0, metadata)
        } catch (_: JSONException) {
            buildError("getSdkStatus", SdkErrorCode.ERR_UNKNOWN, 0, null, null, null, metadata)
        }
    }

    @JvmStatic
    fun buildReportAck(code: String, message: String, metadata: Metadata?): String {
        return try {
            val response = JSONObject()
                .put("status", "received")
                .put("message", message)
                .put("code", code)
            addMetadata(response, metadata)
            response.toString()
        } catch (_: JSONException) {
            "{\"status\":\"received\",\"message\":\"Error report received\"}"
        }
    }

    @JvmStatic
    fun buildError(
        method: String,
        errorCode: SdkErrorCode,
        retryCount: Int,
        logId: String?,
        vendorCode: String?,
        retryable: Boolean?,
        metadata: Metadata?
    ): String {
        return try {
            val error = JSONObject()
                .put("code", errorCode.code)
                .put("message", errorCode.message)
                .put("retryCount", retryCount)
                .put("resolved", false)
            if (!vendorCode.isNullOrBlank()) {
                error.put("vendorCode", vendorCode)
            }
            if (retryable != null) {
                error.put("retryable", retryable)
            }

            val response = JSONObject()
                .put("status", "error")
                .put("method", method)
                .put("error", error)
            if (!logId.isNullOrBlank()) {
                response.put("logId", logId)
            }
            addMetadata(response, metadata)
            response.toString()
        } catch (_: JSONException) {
            "{\"status\":\"error\",\"method\":\"$method\"}"
        }
    }

    private fun buildSuccess(method: String, data: JSONObject, retryCount: Int, metadata: Metadata?): String {
        val response = JSONObject()
            .put("status", "success")
            .put("method", method)
            .put("data", data)
            .put("retryCount", retryCount)
        addMetadata(response, metadata)
        return response.toString()
    }

    private fun addMetadata(response: JSONObject, metadata: Metadata?) {
        if (metadata == null) return
        if (!metadata.callbackId.isNullOrBlank()) response.put("callbackId", metadata.callbackId)
        if (!metadata.correlationId.isNullOrBlank()) response.put("correlationId", metadata.correlationId)
        if (!metadata.platform.isNullOrBlank()) response.put("platform", metadata.platform)
        if (!metadata.stage.isNullOrBlank()) response.put("stage", metadata.stage)
        if (metadata.durationMs != null) response.put("durationMs", metadata.durationMs)
        if (!metadata.scenario.isNullOrBlank()) response.put("scenario", metadata.scenario)
        if (!metadata.vendorCode.isNullOrBlank()) response.put("vendorCode", metadata.vendorCode)
        if (metadata.retryable != null) response.put("retryable", metadata.retryable)
        if (metadata.resolvedByRetry != null) response.put("resolvedByRetry", metadata.resolvedByRetry)
    }

    data class Metadata(
        val callbackId: String?,
        val correlationId: String?,
        val platform: String?,
        val stage: String?,
        val durationMs: Long?,
        val scenario: String? = null,
        val vendorCode: String? = null,
        val retryable: Boolean? = null,
        val resolvedByRetry: Boolean? = null
    )
}
