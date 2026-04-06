package com.demo.railbridge.sdk;

public class ChargeResult {

    private final String transactionId;
    private final int amount;
    private final int balance;
    private final String timestamp;

    public ChargeResult(String transactionId, int amount, int balance, String timestamp) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.balance = balance;
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getAmount() {
        return amount;
    }

    public int getBalance() {
        return balance;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
