package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    @PostMapping("/{orderItemId}/cancel")
    public ResponseApi<Void>  cancelOrder(@PathVariable Long orderItemId, @RequestBody CancelOrderItemRequest request) {
        orderItemService.cancelOrderItem(orderItemId, request);

        return ResponseApi.ok();
    }
}
