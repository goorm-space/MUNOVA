package com.space.munova.order.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record GetOrderListResponse (
    List<OrderSummaryDto> orders,
    int currentPage,
    int totalPages,
    long totalElements
) {
    public static GetOrderListResponse from(Page<OrderSummaryDto> orders) {
        return new GetOrderListResponse(
                orders.getContent(),
                orders.getNumber(),
                orders.getTotalPages(),
                orders.getTotalElements()
        );
    }
}
