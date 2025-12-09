package com.space.munova.order.service;

import com.space.munova.order.entity.Order;
import com.space.munova.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncOrderService {

    private final OrderRepository orderRepository;

    @Async("orderExecutor")
    @Transactional
    public void saveOrderAsync(Order order) {
        orderRepository.save(order);
    }
}
