package com.space.munova.payment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelReason {

    // 주문/결제 취소 사유
    CUSTOMER_SIMPLE_CHANGE("단순 변심"),
    ORDER_MISTAKE("주문 실수"),
    PAYMENT_CHANGE("다른 결제 수단으로 변경"),
    ETC_ORDER_CANCEL("기타"),

    // 반품/환불 사유
    SIZE_MISS("사이즈가 맞지 않음"),
    DELIVERY_PROBLEM("오배송 및 배송 지연"),
    PRODUCT_PROBLEM("상품 파손 및 불량"),

    // 서버 문제
    ROLLBACK_COMPENSATION("서버 롤백으로 인한 취소");

    private final String description;
}
