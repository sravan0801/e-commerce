package com.example.storefront.catalog;

import com.example.storefront.common.BaseEntity;
import com.example.storefront.common.DomainExceptions.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A sellable item. {@code priceCents} is minor units; {@code currency} is ISO-4217.
 * {@code stockQuantity} is decremented by {@code OrderProcessor} when an order is
 * confirmed; the {@code @Version} column makes two orders racing for the last
 * unit collide instead of both succeeding (JPA optimistic locking).
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    public boolean hasStock(int quantity) {
        return stockQuantity >= quantity;
    }

    /** Take {@code quantity} units out of stock, or fail if there aren't enough. */
    public void removeStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new BusinessRuleException("Not enough stock for " + sku);
        }
        stockQuantity -= quantity;
    }
}
