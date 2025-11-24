package com.space.munova.order.controller;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.dto.OrderSummaryDto;
import com.space.munova.order.dto.PaymentPrepareResponse;
import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.service.OrderService;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.product.application.exception.ProductDetailException;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 후 결제에 필요한 응답 보내기
     */
    @PostMapping
    public ResponseApi<PaymentPrepareResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletResponse response
    ) {
        Long memberId = JwtHelper.getMemberId();

        try {
            Order order = orderService.createOrder(request, memberId);
            orderService.saveOrderLog(order);
            PaymentPrepareResponse paymentResponse = PaymentPrepareResponse.from(order);
            return ResponseApi.created(response, paymentResponse);
        } catch (AuthException | OrderException | OrderItemException | ProductDetailException e) {
            return ResponseApi.nok(e.getStatusCode(), e.getCode(), e.getMessage());
        }
    }

    @GetMapping
    public ResponseApi<PagingResponse<OrderSummaryDto>> getOrders(@RequestParam(value = "page", defaultValue = "0") int page) {
        Long memberId = JwtHelper.getMemberId();
        if (page < 0) page = 0;

        PagingResponse<OrderSummaryDto> response = orderService.getOrderList(page, memberId);

        return ResponseApi.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseApi<?> getOrderDetail(@PathVariable("orderId") Long orderId) {
        Long memberId = JwtHelper.getMemberId();

        try {
            GetOrderDetailResponse response = orderService.getOrderDetail(orderId, memberId);
            return ResponseApi.ok(response);
        } catch (OrderException | AuthException | PaymentException e) {
            return ResponseApi.nok(e.getStatusCode(), e.getCode(), e.getMessage());
        }

    }
}
