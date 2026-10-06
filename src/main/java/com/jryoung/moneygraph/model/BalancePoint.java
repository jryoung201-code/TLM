package com.jryoung.moneygraph.model;

import java.math.BigInteger;

public class BalancePoint {
    private String balance = "0";
    private long timestamp;
    public BalancePoint() {}
    public BalancePoint(BigInteger balance, long timestamp) { this.balance = balance.toString(); this.timestamp = timestamp; }
    public BigInteger getBalance() { try { return new BigInteger(balance); } catch (Exception ignored) { return BigInteger.ZERO; } }
    public long getTimestamp() { return timestamp; }
}