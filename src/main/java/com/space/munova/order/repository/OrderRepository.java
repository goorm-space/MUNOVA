package com.space.munova.order.repository;

import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.member m " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "WHERE o.id = :orderId")
    Optional<Order> findOrderDetailsById(@Param("orderId") Long orderId);

    Page<Order> findAllByMember_IdAndStatus(Long memberId, OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o WHERE o.id IN :orderIds ORDER BY o.createdAt DESC")
    @EntityGraph(attributePaths = {
            "orderItems.productDetail.product.brand", // Brand (ToOne)
            "orderItems.productDetail.product.productImages", // ProductImages (ToMany)
            "orderItems.productDetail.optionMappings.option"  // Options (ToMany)
    })
    List<Order> findAllWithDetailsByOrderIds(@Param("orderIds") List<Long> orderIds);

    Optional<Order> findByOrderNum(String orderNum);

    List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime before);
}
