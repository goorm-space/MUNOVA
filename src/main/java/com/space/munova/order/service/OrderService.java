package com.space.munova.order.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.dto.OrderSummaryDto;
import com.space.munova.order.entity.Order;

public interface OrderService {

    Order saveTmpOrder(CreateOrderRequest request, Long memberId);

    PagingResponse<OrderSummaryDto> getOrderList(int page, Long memberId);

    GetOrderDetailResponse getOrderDetail(Long orderId, Long memberId);

    void saveOrder(Order order);

    void saveOrderLog(Long memberId, Order order);
}
