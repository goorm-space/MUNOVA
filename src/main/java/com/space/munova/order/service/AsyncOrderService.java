package com.space.munova.order.service;

import com.space.munova.order.entity.Order;
import com.space.munova.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaveInitOrderService {

    private final OrderRepository orderRepository;

    @Async
    public CompletableFuture<Void> saveOrderAsync(Order order) {
        log.error("rdb 시작");
        orderRepository.save(order);
        log.error("rdb 완료");
        return CompletableFuture.completedFuture(null);
    }
}
