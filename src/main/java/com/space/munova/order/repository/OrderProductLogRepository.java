package com.space.munova.order.repository;

import com.space.munova.order.entity.OrderProductLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductLogRepository extends JpaRepository<OrderProductLog, Long> {
}
