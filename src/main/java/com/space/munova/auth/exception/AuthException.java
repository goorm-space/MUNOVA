package com.space.munova.auth.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class AuthException extends BaseException {

    public AuthException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static AuthException duplicateUsernameException(String... detailMessage) {
        return new AuthException("AUTH_01", "이미 존재하는 사용자명입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }
}
