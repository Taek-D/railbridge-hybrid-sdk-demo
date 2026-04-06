package com.demo.railbridge.bridge;

import com.demo.railbridge.sdk.BalanceSnapshot;
import com.demo.railbridge.sdk.ChargeResult;
import com.demo.railbridge.sdk.SdkErrorCode;
import com.demo.railbridge.sdk.SdkStatusSnapshot;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BridgeResponseFactoryTest {

    @Test
    public void chargeSuccess_keepsBaseFieldsAndAddsMetadata() throws Exception {
        ChargeResult result = new ChargeResult("TXN_123", 10000, 45000, "2026-04-06T11:00:00Z");
        String json = BridgeResponseFactory.buildChargeSuccess(
                result,
                new BridgeResponseFactory.Metadata("cb-1", "corr-1", "android", "sdk_callback", 123L)
        );

        JSONObject object = new JSONObject(json);
        assertEquals("success", object.getString("status"));
        assertEquals("requestCharge", object.getString("method"));
        assertEquals(0, object.getInt("retryCount"));
        assertEquals("cb-1", object.getString("callbackId"));
        assertEquals("corr-1", object.getString("correlationId"));
        assertEquals("android", object.getString("platform"));
        assertEquals("sdk_callback", object.getString("stage"));
        assertEquals(123L, object.getLong("durationMs"));
        assertEquals("TXN_123", object.getJSONObject("data").getString("transactionId"));
    }

    @Test
    public void balanceSuccess_keepsCurrentContract() throws Exception {
        BalanceSnapshot result = new BalanceSnapshot("CARD_001", 38947, "2026-04-06T11:01:37Z");
        String json = BridgeResponseFactory.buildBalanceSuccess(result, null);

        JSONObject object = new JSONObject(json);
        assertEquals("success", object.getString("status"));
        assertEquals("getBalance", object.getString("method"));
        assertEquals(0, object.getInt("retryCount"));
        assertEquals("CARD_001", object.getJSONObject("data").getString("cardId"));
        assertFalse(object.has("callbackId"));
    }

    @Test
    public void statusSuccess_staysParseableByCurrentWebViewHandler() throws Exception {
        String json = BridgeResponseFactory.buildStatusSuccess(
                new SdkStatusSnapshot(true, "1.2.3-mock"),
                new BridgeResponseFactory.Metadata(null, "corr-2", "android", null, null)
        );

        JSONObject object = new JSONObject(json);
        assertEquals("success", object.getString("status"));
        assertEquals("getSdkStatus", object.getString("method"));
        assertEquals(true, object.getJSONObject("data").getBoolean("initialized"));
        assertEquals("1.2.3-mock", object.getJSONObject("data").getString("version"));
        assertEquals("corr-2", object.getString("correlationId"));
    }

    @Test
    public void error_keepsNestedRetryCountAndAddsVendorMetadata() throws Exception {
        String json = BridgeResponseFactory.buildError(
                "getBalance",
                SdkErrorCode.RETRY_EXHAUSTED,
                3,
                "log-1",
                "VENDOR_TIMEOUT",
                true,
                new BridgeResponseFactory.Metadata(null, "corr-3", "android", "final_error", 2400L)
        );

        JSONObject object = new JSONObject(json);
        JSONObject error = object.getJSONObject("error");
        assertEquals("error", object.getString("status"));
        assertEquals("getBalance", object.getString("method"));
        assertEquals("9001", error.getString("code"));
        assertEquals(3, error.getInt("retryCount"));
        assertEquals("VENDOR_TIMEOUT", error.getString("vendorCode"));
        assertTrue(error.getBoolean("retryable"));
        assertEquals("log-1", object.getString("logId"));
        assertEquals("corr-3", object.getString("correlationId"));
    }

    @Test
    public void reportAck_remainsCompatibleWithReceivedBranch() throws Exception {
        String json = BridgeResponseFactory.buildReportAck(
                "ERR_TEST_001",
                "Error report received",
                new BridgeResponseFactory.Metadata(null, null, "android", "ack", 8L)
        );

        JSONObject object = new JSONObject(json);
        assertEquals("received", object.getString("status"));
        assertEquals("ERR_TEST_001", object.getString("code"));
        assertEquals("Error report received", object.getString("message"));
        assertEquals("android", object.getString("platform"));
        assertEquals("ack", object.getString("stage"));
    }
}
