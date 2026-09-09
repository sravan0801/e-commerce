package com.example.storefront.order;

/**
 * <pre>
 * PENDING ──(OrderProcessor: stock ok + payment ok)──▶ CONFIRMED
 *    └─────(OrderProcessor: out of stock / payment declined)──▶ REJECTED
 * </pre>
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}
