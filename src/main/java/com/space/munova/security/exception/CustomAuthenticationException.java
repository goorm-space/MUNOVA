package com.space.munova.security.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class CustomAuthenticationException extends AuthenticationException {

    String errorCode;

    public CustomAuthenticationException(String msg) {
        super(msg);
    }

    public CustomAuthenticationException(String msg, String errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }
}
