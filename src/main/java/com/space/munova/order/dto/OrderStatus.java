package com.space.munova.order.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    // orders
    CREATED("주문서 생성"),
    PAYMENT_PENDING("결제 대기"),
    PAYMENT_FAILED("결제 실패"),
    PAID("결제 완료"),

    // order_item
    SHIPPING_READY("배송 준비 중"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료"),
    CANCELED("주문 취소"),
    RETURN_REQUESTED("반품 요청"),
    RETURNED("반품 완료"),
    EXCHANGE_REQUESTED("교환 요청"),
    EXCHANGED("교환 완료"),
    REFUNDED("환불 완료"),
    CONFIRMED("구매 확정");

    private final String description;
}
