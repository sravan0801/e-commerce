package com.example.storefront.order;

import com.example.storefront.catalog.Product;
import com.example.storefront.catalog.ProductRepository;
import com.example.storefront.config.KafkaTopicConfig;
import com.example.storefront.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes {@code orders.placed} and finishes the order: check every line has
 * stock, take the stock, run a stand-in payment, then mark the order CONFIRMED
 * or REJECTED. All in one transaction, so a failure rolls the whole thing back
 * and Kafka re-delivers the message.
 */
@Component
public class OrderProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    /** Orders this size or larger are "declined" by the stand-in payment step. */
    private static final long PAYMENT_LIMIT_CENTS = 1_000_000L;

    private final OrderRepository orders;
    private final ProductRepository products;

    public OrderProcessor(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    @KafkaListener(topics = KafkaTopicConfig.ORDERS_PLACED, groupId = "order-processor")
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event) {
        Order order = orders.findByOrderRef(event.orderRef()).orElse(null);
        if (order == null) {
            log.warn("No order {} -- ignoring event", event.orderRef());
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            return; // already handled (e.g. a redelivered message)
        }

        for (OrderPlacedEvent.Line line : event.lines()) {
            Product product = products.findById(line.productId()).orElse(null);
            if (product == null || !product.hasStock(line.quantity())) {
                order.markRejected("Out of stock: product " + line.productId());
                log.info("{} -> REJECTED (stock)", order.getOrderRef());
                return;
            }
        }
        if (order.getTotalCents() >= PAYMENT_LIMIT_CENTS) {
            order.markRejected("Payment declined: amount too large");
            log.info("{} -> REJECTED (payment)", order.getOrderRef());
            return;
        }

        for (OrderPlacedEvent.Line line : event.lines()) {
            products.findById(line.productId()).orElseThrow().removeStock(line.quantity());
        }
        order.markConfirmed();
        log.info("{} -> CONFIRMED", order.getOrderRef());
    }
}
