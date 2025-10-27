package com.space.munova.order.service;

import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import org.springframework.web.bind.annotation.RequestBody;

public interface OrderService {

    Order createOrder(CreateOrderRequest request);
    GetOrderListResponse getOrderList(Long userId, int page);
    Order getOrderDetail(Long orderId);
}
