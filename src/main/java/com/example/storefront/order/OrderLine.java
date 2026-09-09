package com.example.storefront.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A purchased item. sku and unit price are snapshotted at order time. */
@Entity
@Table(name = "order_line")
@Getter
@NoArgsConstructor
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String sku;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(nullable = false)
    private int quantity;

    public OrderLine(Order order, Long productId, String sku, long unitPriceCents, int quantity) {
        this.order = order;
        this.productId = productId;
        this.sku = sku;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }
}
