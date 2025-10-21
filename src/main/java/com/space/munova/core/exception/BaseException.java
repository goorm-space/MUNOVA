package com.space.munova.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class BaseException extends RuntimeException {

    private final String code;
    private final String message;
    private final String detailMessage;
    private final HttpStatusCode statusCode;

    public BaseException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(message);
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;

        if (detailMessage == null || detailMessage.length == 0) {
            this.detailMessage = null;
        } else {
            String joinedMessage = Arrays.stream(detailMessage)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.joining(" "));
            this.detailMessage = joinedMessage.isEmpty() ? null : joinedMessage;
        }
    }
}
