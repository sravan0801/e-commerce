package com.example.storefront.event;

import java.time.Instant;
import java.util.List;

/**
 * Published to Kafka when an order row is created. A single consumer
 * ({@code OrderProcessor}) reads it and drives the order to CONFIRMED or
 * REJECTED. Carries everything the consumer needs so it never has to call back
 * into the order module for data.
 */
public record OrderPlacedEvent(
        String orderRef,
        Long userId,
        String currency,
        long totalCents,
        Instant placedAt,
        List<Line> lines) {

    public record Line(Long productId, String sku, int quantity) {
    }
}
