package com.example.storefront.order;

import com.example.storefront.catalog.Product;
import com.example.storefront.catalog.ProductRepository;
import com.example.storefront.common.DomainExceptions.BusinessRuleException;
import com.example.storefront.common.DomainExceptions.NotFoundException;
import com.example.storefront.config.KafkaTopicConfig;
import com.example.storefront.event.OrderPlacedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates orders. {@link #placeOrder} saves the order as {@code PENDING}, then
 * publishes an {@link OrderPlacedEvent} to Kafka; {@code OrderProcessor} picks it
 * up and decides CONFIRMED / REJECTED. The HTTP call returns as soon as the row
 * is saved.
 *
 * <p>The order row is saved (and committed) before the event is published, so
 * the consumer never sees an event for an order it can't load yet. The remaining
 * gap -- a crash after the commit but before the publish -- would leave an order
 * stuck at PENDING; a production system closes that with the transactional-outbox
 * pattern. Left out on purpose.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ProductRepository products;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderService(OrderRepository orders, ProductRepository products,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.orders = orders;
        this.products = products;
        this.kafkaTemplate = kafkaTemplate;
    }

    public record ItemCommand(Long productId, int quantity) {
    }

    public Order placeOrder(Long userId, List<ItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException("An order needs at least one line.");
        }
        List<Long> productIds = items.stream().map(ItemCommand::productId).distinct().toList();
        Map<Long, Product> catalogue = products.findByIdInAndActiveTrue(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        if (catalogue.size() != productIds.size()) {
            throw new BusinessRuleException("One or more products are unavailable.");
        }
        Set<String> currencies = catalogue.values().stream()
                .map(Product::getCurrency).collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw new BusinessRuleException("All items in an order must share a currency.");
        }

        Order order = new Order(nextOrderRef(), userId, currencies.iterator().next());
        for (ItemCommand item : items) {
            if (item.quantity() <= 0) {
                throw new BusinessRuleException("Quantity must be positive.");
            }
            Product p = catalogue.get(item.productId());
            order.addLine(p.getId(), p.getSku(), p.getPriceCents(), item.quantity());
        }
        // save() runs in its own transaction and commits before it returns, so
        // the row exists by the time the consumer reads the event below.
        orders.save(order);
        kafkaTemplate.send(KafkaTopicConfig.ORDERS_PLACED, order.getOrderRef(), toEvent(order));
        log.info("Placed {} for user {} ({} lines, {} {})", order.getOrderRef(), userId,
                order.getLines().size(), order.getTotalCents(), order.getCurrency());
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> ordersOf(Long userId, Pageable pageable) {
        return orders.findByUserIdOrderByIdDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Order requireForUser(String orderRef, Long userId) {
        Order order = orders.findByOrderRef(orderRef)
                .orElseThrow(() -> new NotFoundException("Order " + orderRef + " not found."));
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException("Order " + orderRef + " not found.");
        }
        return order;
    }

    private OrderPlacedEvent toEvent(Order order) {
        List<OrderPlacedEvent.Line> lines = order.getLines().stream()
                .map(l -> new OrderPlacedEvent.Line(l.getProductId(), l.getSku(), l.getQuantity()))
                .toList();
        return new OrderPlacedEvent(order.getOrderRef(), order.getUserId(), order.getCurrency(),
                order.getTotalCents(), Instant.now(), lines);
    }

    private static String nextOrderRef() {
        String stamp = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD-" + stamp + "-" + suffix;
    }
}
