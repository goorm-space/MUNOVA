package com.space.munova.order.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    Order createOrder(CreateOrderRequest request, Long memberId);
    PagingResponse<OrderSummaryDto> getOrderList(int page, Long memberId);
    GetOrderDetailResponse getOrderDetail(Long orderId, Long memberId);
    void saveOrderLog(Order order);
}
