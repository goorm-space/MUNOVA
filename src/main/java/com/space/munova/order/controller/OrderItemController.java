package com.space.munova.order.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.order.service.OrderItemService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    @PatchMapping("/{orderItemId}/cancel")
    public ResponseApi<Void>  cancelOrder(@PathVariable Long orderItemId) {
        orderItemService.updateStatusAndCancel(orderItemId);

        return ResponseApi.ok();
    }
}
