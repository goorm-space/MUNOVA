package com.space.munova.order.service;

import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.CreateOrderResponse;
import com.space.munova.order.entity.Order;

public interface OrderService {

    Order createOrder(Long userId, CreateOrderRequest request);
}
