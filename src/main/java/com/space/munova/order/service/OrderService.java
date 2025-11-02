package com.space.munova.order.service;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    Order createOrder(CreateOrderRequest request);
    Page<OrderSummaryDto> getOrdersByMember(Long memberId, OrderStatus orderStatus, Pageable pageable);
    PagingResponse<OrderSummaryDto> getOrderList(int page);
    GetOrderDetailResponse getOrderDetail(Long orderId);
    void saveOrderLog(Order order);
}
