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

    public static AuthException invalidTokenException(String... detailMessage) {
        return new AuthException("AUTH_02", "토큰이 유효하지 않습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED, detailMessage);
    }

    public static AuthException tokenExpiredException(String... detailMessage) {
        return new AuthException("AUTH_03", "토큰이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED, detailMessage);
    }

    public static AuthException unauthorizedException(String... detailMessage) {
        return new AuthException("AUTH_04", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN, detailMessage);
    }
}
