package com.example.storefront.order;

import com.example.storefront.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "order_ref", nullable = false, unique = true)
    private String orderRef;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "rejection_note")
    private String rejectionNote;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    public Order(String orderRef, Long userId, String currency) {
        this.orderRef = orderRef;
        this.userId = userId;
        this.currency = currency;
    }

    public void addLine(Long productId, String sku, long unitPriceCents, int quantity) {
        lines.add(new OrderLine(this, productId, sku, unitPriceCents, quantity));
        this.totalCents += unitPriceCents * (long) quantity;
    }

    public void markConfirmed() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void markRejected(String reason) {
        this.status = OrderStatus.REJECTED;
        this.rejectionNote = reason;
    }
}
