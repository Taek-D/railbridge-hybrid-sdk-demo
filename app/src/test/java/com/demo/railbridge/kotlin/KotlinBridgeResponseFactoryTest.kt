package com.demo.railbridge.kotlin

import com.demo.railbridge.sdk.BalanceSnapshot
import com.demo.railbridge.sdk.ChargeResult
import com.demo.railbridge.sdk.SdkErrorCode
import com.demo.railbridge.sdk.SdkStatusSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinBridgeResponseFactoryTest {

    @Test
    fun chargeSuccess_keepsBaseFieldsAndAddsMetadata() {
        val result = ChargeResult("TXN_123", 10_000, 45_000, "2026-04-06T11:00:00Z")
        val json = KotlinBridgeResponseFactory.buildChargeSuccess(
            result,
            0,
            KotlinBridgeResponseFactory.Metadata("cb-1", "corr-1", "android", "sdk_callback", 123L)
        )

        val `object` = JSONObject(json)
        assertEquals("success", `object`.getString("status"))
        assertEquals("requestCharge", `object`.getString("method"))
        assertEquals(0, `object`.getInt("retryCount"))
        assertEquals("cb-1", `object`.getString("callbackId"))
        assertEquals("corr-1", `object`.getString("correlationId"))
        assertEquals("android", `object`.getString("platform"))
        assertEquals("sdk_callback", `object`.getString("stage"))
        assertEquals(123L, `object`.getLong("durationMs"))
        assertEquals("TXN_123", `object`.getJSONObject("data").getString("transactionId"))
    }

    @Test
    fun balanceSuccess_keepsCurrentContract() {
        val result = BalanceSnapshot("CARD_001", 38_947, "2026-04-06T11:01:37Z")
        val json = KotlinBridgeResponseFactory.buildBalanceSuccess(result, 0, null)

        val `object` = JSONObject(json)
        assertEquals("success", `object`.getString("status"))
        assertEquals("getBalance", `object`.getString("method"))
        assertEquals(0, `object`.getInt("retryCount"))
        assertEquals("CARD_001", `object`.getJSONObject("data").getString("cardId"))
        assertFalse(`object`.has("callbackId"))
    }

    @Test
    fun statusSuccess_staysParseableByCurrentWebViewHandler() {
        val json = KotlinBridgeResponseFactory.buildStatusSuccess(
            SdkStatusSnapshot(true, "1.2.3-mock"),
            KotlinBridgeResponseFactory.Metadata(null, "corr-2", "android", null, null)
        )

        val `object` = JSONObject(json)
        assertEquals("success", `object`.getString("status"))
        assertEquals("getSdkStatus", `object`.getString("method"))
        assertTrue(`object`.getJSONObject("data").getBoolean("initialized"))
        assertEquals("1.2.3-mock", `object`.getJSONObject("data").getString("version"))
        assertEquals("corr-2", `object`.getString("correlationId"))
    }

    @Test
    fun error_keepsNestedRetryCountAndAddsVendorMetadata() {
        val json = KotlinBridgeResponseFactory.buildError(
            "getBalance",
            SdkErrorCode.RETRY_EXHAUSTED,
            3,
            "log-1",
            "VENDOR_TIMEOUT",
            true,
            KotlinBridgeResponseFactory.Metadata(null, "corr-3", "android", "final_error", 2400L)
        )

        val `object` = JSONObject(json)
        val error = `object`.getJSONObject("error")
        assertEquals("error", `object`.getString("status"))
        assertEquals("getBalance", `object`.getString("method"))
        assertEquals("9001", error.getString("code"))
        assertEquals(3, error.getInt("retryCount"))
        assertEquals("VENDOR_TIMEOUT", error.getString("vendorCode"))
        assertTrue(error.getBoolean("retryable"))
        assertEquals("log-1", `object`.getString("logId"))
        assertEquals("corr-3", `object`.getString("correlationId"))
    }

    @Test
    fun reportAck_remainsCompatibleWithReceivedBranch() {
        val json = KotlinBridgeResponseFactory.buildReportAck(
            "ERR_TEST_001",
            "Error report received",
            KotlinBridgeResponseFactory.Metadata(null, null, "android", "ack", 8L)
        )

        val `object` = JSONObject(json)
        assertEquals("received", `object`.getString("status"))
        assertEquals("ERR_TEST_001", `object`.getString("code"))
        assertEquals("Error report received", `object`.getString("message"))
        assertEquals("android", `object`.getString("platform"))
        assertEquals("ack", `object`.getString("stage"))
    }
}
