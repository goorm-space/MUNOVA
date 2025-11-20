package com.space.munova.order.service.cancel;

import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelStrategy implements CancellationStrategy{

    private static final OrderStatus REQUIRED_STATUS = OrderStatus.PAID;

    @Override
    public void validate(OrderStatus currentStatus) {
        if (currentStatus != REQUIRED_STATUS) {
            throw OrderItemException.cancellationNotAllowedException(
                    String.format("주문 취소는 '%s' 상태에서만 가능합니다. 현재 상태: %s", REQUIRED_STATUS, currentStatus)
            );
        }
    }

    @Override
    public void updateOrderItemStatus(OrderItem orderItem) {
        orderItem.updateStatus(OrderStatus.CANCELED);
    }
}
