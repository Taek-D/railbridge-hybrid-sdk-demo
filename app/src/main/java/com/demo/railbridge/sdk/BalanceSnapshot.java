package com.demo.railbridge.sdk;

public class BalanceSnapshot {

    private final String cardId;
    private final int balance;
    private final String timestamp;

    public BalanceSnapshot(String cardId, int balance, String timestamp) {
        this.cardId = cardId;
        this.balance = balance;
        this.timestamp = timestamp;
    }

    public String getCardId() {
        return cardId;
    }

    public int getBalance() {
        return balance;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
