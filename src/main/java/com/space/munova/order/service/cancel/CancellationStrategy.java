package com.space.munova.order.service.cancel;

import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.OrderItem;

public interface CancellationStrategy {
    void validate(OrderStatus orderStatus);
    void updateOrderItemStatus(OrderItem orderItem);
}
