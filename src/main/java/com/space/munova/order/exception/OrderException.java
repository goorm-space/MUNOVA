package com.space.munova.order.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class OrderException extends BaseException {

    public OrderException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static OrderException notFoundException(String... detailMessage) {
        return new OrderException("ORDER_01", "주문정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }
}
