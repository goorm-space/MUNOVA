package com.space.munova.order.service;

import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.CreateOrderResponse;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.dto.GetOrderListResponse;
import com.space.munova.order.entity.Order;

public interface OrderService {

    Order createOrder(Long userId, CreateOrderRequest request);
    GetOrderListResponse getOrderList(int page);
    GetOrderDetailResponse getOrderDetail(Long orderId);
}
