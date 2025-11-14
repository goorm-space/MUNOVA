package com.space.munova.product.application.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class OptionException extends BaseException {
    public OptionException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }


    public static OptionException badRequset(String... detailMessage) {
        return new OptionException("OPTION_01", "유효하지 않은 요청 : ", HttpStatus.NOT_FOUND, detailMessage);
    }
}
