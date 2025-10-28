package com.space.munova.order.repository;

import com.space.munova.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi.productDetail.id FROM OrderItem oi WHERE oi.id IN :orderItemIds")
    List<Long> findProductDetailIdsByOrderItemIds(@Param("orderItemIds") List<Long> orderItemIds);
}
