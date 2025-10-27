package com.space.munova.order.service;

import com.space.munova.order.dto.CancelOrderItemRequest;

public interface OrderItemService {
    void cancelOrderItem(Long orderItemId, CancelOrderItemRequest request);
}
