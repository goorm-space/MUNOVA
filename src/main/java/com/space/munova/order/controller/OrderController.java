package com.space.munova.order.controller;

import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        // Todo: userId 가져오기
        Long userId = 1L;
        Order order = orderService.createOrder(userId, request);
        return ResponseEntity.ok(order);
    }
}
