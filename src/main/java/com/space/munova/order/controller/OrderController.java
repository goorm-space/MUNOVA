package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.dto.OrderSummaryDto;
import com.space.munova.order.dto.PaymentPrepareResponse;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
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
        String userName = JwtHelper.getMemberName();

        Order order = orderService.saveTmpOrder(request, memberId);
        PaymentPrepareResponse paymentResponse = PaymentPrepareResponse.from(userName, order);
        return ResponseApi.created(response, paymentResponse);
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

        GetOrderDetailResponse response = orderService.getOrderDetail(orderId, memberId);
        return ResponseApi.ok(response);
    }
}
