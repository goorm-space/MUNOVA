package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.CreateOrderResponse;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.dto.GetOrderListResponse;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseApi<?> createOrder(@RequestBody CreateOrderRequest request) {
        Long userId = JwtHelper.getMemberId();

        Order order = orderService.createOrder(userId, request);

        CreateOrderResponse response = CreateOrderResponse.from(order);
        return ResponseApi.created(response);
    }

    @GetMapping
    public ResponseApi<?> getOrders(@RequestParam(value = "page", defaultValue = "0") int page) {
        Long userId = JwtHelper.getMemberId();

        GetOrderListResponse response = orderService.getOrderList(userId, page);

        return ResponseApi.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseApi<?> getOrderDetail(@PathVariable("orderId") Long orderId) {
        Long userId = JwtHelper.getMemberId();

        GetOrderDetailResponse response = orderService.getOrderDetail(userId, orderId);

        return ResponseApi.ok(response);
    }
}
