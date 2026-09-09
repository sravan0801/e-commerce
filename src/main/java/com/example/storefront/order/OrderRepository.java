package com.example.storefront.order;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<Order> findByOrderRef(String orderRef);

    Page<Order> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
