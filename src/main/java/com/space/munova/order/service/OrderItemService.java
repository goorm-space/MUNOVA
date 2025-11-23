package com.space.munova.order.service;

import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;

import java.util.List;

public interface OrderItemService {
    List<OrderItem> deductStockAndCreateOrderItems(List<OrderItemRequest> orderItems, Order order);
    void cancelOrderItem(Long orderItemId, CancelOrderItemRequest request, Long memberId);
}
