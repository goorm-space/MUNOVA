package com.space.munova.product.application.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class CartException extends BaseException {
    public CartException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static ProductException notFoundProductException(String... detailMessage) {
        return new ProductException("CART_01", "유효하지 않은 요청입니다. : ", HttpStatus.BAD_REQUEST, detailMessage);
    }

}
