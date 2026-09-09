package com.example.storefront.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.storefront.catalog.Product;
import com.example.storefront.catalog.ProductRepository;
import com.example.storefront.security.AppUser;
import com.example.storefront.security.AppUserRepository;
import com.example.storefront.support.AbstractIntegrationTest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end: placing an order publishes to Kafka, {@code OrderProcessor}
 * consumes it, and the order ends up CONFIRMED with stock decremented -- no
 * direct call from the web layer into the processor.
 */
class OrderProcessingIntegrationTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;
    @Autowired AppUserRepository userRepository;

    @Test
    void placedOrderIsConfirmedByTheProcessor() {
        AppUser buyer = userRepository.save(
                AppUser.create("buyer@test.local", "{noop}irrelevant", "Test Buyer"));
        Product product = productRepository.findBySku("BK-1001").orElseThrow();
        int startingStock = product.getStockQuantity();

        Order placed = orderService.placeOrder(buyer.getId(),
                List.of(new OrderService.ItemCommand(product.getId(), 2)));
        assertThat(placed.getStatus()).isEqualTo(OrderStatus.PENDING);

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            Order current = orderRepository.findByOrderRef(placed.getOrderRef()).orElseThrow();
            assertThat(current.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(startingStock - 2);
    }

    @Test
    void orderIsRejectedWhenStockIsShort() {
        AppUser buyer = userRepository.save(
                AppUser.create("buyer2@test.local", "{noop}irrelevant", "Test Buyer 2"));
        Product product = productRepository.findBySku("BK-1002").orElseThrow();

        Order placed = orderService.placeOrder(buyer.getId(),
                List.of(new OrderService.ItemCommand(product.getId(), product.getStockQuantity() + 1)));

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            Order current = orderRepository.findByOrderRef(placed.getOrderRef()).orElseThrow();
            assertThat(current.getStatus()).isEqualTo(OrderStatus.REJECTED);
        });
    }
}
