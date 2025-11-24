package com.space.munova.order.service.cancel;

import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import org.springframework.stereotype.Component;

@Component
public class ReturnRefundStrategy implements CancellationStrategy {

    private static final OrderStatus REQUIRED_STATUS_1 = OrderStatus.SHIPPING;
    private static final OrderStatus REQUIRED_STATUS_2 = OrderStatus.DELIVERED;

    @Override
    public void validate(OrderStatus currentStatus) {
        if (currentStatus != REQUIRED_STATUS_1 && currentStatus != REQUIRED_STATUS_2) {
            throw OrderItemException.cancellationNotAllowedException(
                    String.format("반품은 '%s' 또는 '%s' 상태에서만 가능합니다. 현재 상태: %s",REQUIRED_STATUS_1, REQUIRED_STATUS_2, currentStatus)
            );
        }
    }

    @Override
    public void updateOrderItemStatus(OrderItem orderItem) {
        orderItem.updateStatus(OrderStatus.REFUNDED);
    }
}
