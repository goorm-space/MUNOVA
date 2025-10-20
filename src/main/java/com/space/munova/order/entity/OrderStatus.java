package com.space.munova.order.entity;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    SHIPPING_READY,
    SHIPPING,
    DELIVERED,
    CANCELED,
    REFUNDED;
}
