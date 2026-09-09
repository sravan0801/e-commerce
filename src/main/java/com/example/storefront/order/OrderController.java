package com.example.storefront.order;

import com.example.storefront.common.PageResponse;
import com.example.storefront.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@AuthenticationPrincipal AppUserPrincipal me,
                           @Valid @RequestBody PlaceOrderRequest body) {
        List<OrderService.ItemCommand> items = body.items().stream()
                .map(i -> new OrderService.ItemCommand(i.productId(), i.quantity()))
                .toList();
        return OrderView.from(orders.placeOrder(me.getUserId(), items));
    }

    @GetMapping
    public PageResponse<OrderSummaryView> myOrders(@AuthenticationPrincipal AppUserPrincipal me,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(orders.ordersOf(me.getUserId(), pageable), OrderSummaryView::from);
    }

    @GetMapping("/{orderRef}")
    public OrderView one(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable String orderRef) {
        return OrderView.from(orders.requireForUser(orderRef, me.getUserId()));
    }

    // --- DTOs -----------------------------------------------------------

    public record PlaceOrderRequest(@NotEmpty @Valid List<Item> items) {
        public record Item(@NotNull Long productId, @Positive int quantity) {
        }
    }

    public record OrderView(
            String orderRef, String status, long totalCents, String currency,
            String rejectionNote, List<LineView> lines) {

        static OrderView from(Order o) {
            return new OrderView(o.getOrderRef(), o.getStatus().name(), o.getTotalCents(),
                    o.getCurrency(), o.getRejectionNote(),
                    o.getLines().stream().map(LineView::from).toList());
        }
    }

    public record LineView(Long productId, String sku, long unitPriceCents, int quantity, long lineTotalCents) {
        static LineView from(OrderLine l) {
            return new LineView(l.getProductId(), l.getSku(), l.getUnitPriceCents(),
                    l.getQuantity(), l.lineTotalCents());
        }
    }

    /** Lightweight row for the list endpoint -- no line items, so no lazy load. */
    public record OrderSummaryView(String orderRef, String status, long totalCents, String currency) {
        static OrderSummaryView from(Order o) {
            return new OrderSummaryView(o.getOrderRef(), o.getStatus().name(),
                    o.getTotalCents(), o.getCurrency());
        }
    }
}
