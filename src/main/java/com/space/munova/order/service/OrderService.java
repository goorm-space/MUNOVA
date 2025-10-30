package com.space.munova.order.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;

public interface OrderService {

    Order createOrder(CreateOrderRequest request);
    PagingResponse<OrderSummaryDto> getOrderList(int page);
    Order getOrderDetail(Long orderId);
    void saveOrderLog(Order order);
}
