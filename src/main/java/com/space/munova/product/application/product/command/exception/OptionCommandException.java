package com.space.munova.product.application.product.command.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class OptionCommandException extends BaseException {
    public OptionCommandException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }


    public static OptionCommandException badRequset(String... detailMessage) {
        return new OptionCommandException("OPTION_01", "유효하지 않은 요청 : ", HttpStatus.NOT_FOUND, detailMessage);
    }
}
