package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * 주문 생성 후 결제에 필요한 응답 보내기
     */
    @PostMapping
    public ResponseApi<PaymentPrepareResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Long memberId = JwtHelper.getMemberId();
        Order order = orderService.createOrder(request, memberId);
        orderService.saveOrderLog(order);
        PaymentPrepareResponse response = PaymentPrepareResponse.from(order);

        return ResponseApi.created(response);
    }

    @GetMapping
    public ResponseApi<PagingResponse<OrderSummaryDto>> getOrders(@RequestParam(value = "page", defaultValue = "0") int page) {
        PagingResponse<OrderSummaryDto> response = orderService.getOrderList(page);
        System.out.print(response.content());

        return ResponseApi.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseApi<?> getOrderDetail(@PathVariable("orderId") Long orderId) {
        GetOrderDetailResponse response = orderService.getOrderDetail(orderId);

        return ResponseApi.ok(response);
    }
}
