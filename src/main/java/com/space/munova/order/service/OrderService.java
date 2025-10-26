package com.space.munova.order.service;

import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;

public interface OrderService {

    Order createTmpOrder(CreateOrderRequest request);
    Order confirmOrder(ConfirmOrderRequest request);
    GetOrderListResponse getOrderList(Long userId, int page);
    Order getOrderDetail(Long orderId);
}
