package com.space.munova.order.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class OrderItemException extends BaseException {

    public OrderItemException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static OrderItemException notFoundException(String... detailMessage) {
        return new OrderItemException("ORDER_ITEM_01", "주문 상품을 찾을 수 없습니다", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static OrderItemException cancellationNotAllowedException(String... detailMessage) {
        return new OrderItemException("ORDER_ITEM_02", "주문을 취소할 수 없습니다.", HttpStatus.CONFLICT, detailMessage);
    }
}
