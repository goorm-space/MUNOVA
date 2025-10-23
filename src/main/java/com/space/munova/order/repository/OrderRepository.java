package com.space.munova.order.repository;

import com.space.munova.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.member m " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "WHERE o.id = :orderId")
    Optional<Order> findOrderDetailsById(@Param("orderId") Long orderId);

    Page<Order> findAll(Pageable pageable);
}
