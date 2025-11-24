package com.space.munova.order.service.processor;

import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;

public interface OrderAmountProcessor {
    void process(Order order, CreateOrderRequest request, long totalAmount);
}
