package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.CreateOrderResponse;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseApi<?> createOrder(@RequestBody CreateOrderRequest request) {
        // Todo: userId 가져오기
        Long userId = 1L;
        Order order = orderService.createOrder(userId, request);
        CreateOrderResponse response = CreateOrderResponse.from(order);
        return ResponseApi.created(response);
    }

    @GetMapping("/{orderId}")
    public ResponseApi<?> getOrderDetail(@PathVariable("orderId") Long orderId) {

        GetOrderDetailResponse response = orderService.getOrderDetail(orderId);

        return ResponseApi.ok(response);
    }
}
