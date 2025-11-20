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

    public static OrderItemException noOrderItemsNotAllowedException(String... detailMessage) {
        return new OrderItemException("ORDER_ITEM_03", "주문 상품이 없습니다. 상품을 추가해주세요.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static OrderItemException invalidItemId(String... detailMessage) {
        return new OrderItemException("ORDER_ITEM_04", "주문 항목 ID가 유효하지 않습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static OrderItemException invalidQuantity(String... detailMessage) {
        return new OrderItemException("ORDER_ITEM_05", "주문 수량은 1 이상이어야 합니다.",  HttpStatus.BAD_REQUEST, detailMessage);
    }
}
