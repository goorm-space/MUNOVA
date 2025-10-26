package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseApi<CreateOrderResponse> createTmpOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createTmpOrder(request);

        CreateOrderResponse response = CreateOrderResponse.from(order);

        return ResponseApi.created(response);
    }

    @PatchMapping
    public ResponseApi<?> confirmOrder(@RequestBody ConfirmOrderRequest request) {
        Order order = orderService.confirmOrder(request);

        PaymentPrepareResponse response = PaymentPrepareResponse.from(order);
        return ResponseApi.ok(response);
    }

    @GetMapping
    public ResponseApi<?> getOrders(@RequestParam(value = "page", defaultValue = "0") int page) {
        Long userId = JwtHelper.getMemberId();

        GetOrderListResponse response = orderService.getOrderList(userId, page);

        return ResponseApi.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseApi<?> getOrderDetail(@PathVariable("orderId") Long orderId) {
        Order order = orderService.getOrderDetail(orderId);

        GetOrderDetailResponse response = GetOrderDetailResponse.from(order);

        return ResponseApi.ok(response);
    }
}
