package com.space.munova.core.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseApi<T> {

    private int statusCode;
    private String code;
    private String message;
    private T data;

    private ResponseApi(int statusCode, T data) {
        this.statusCode = statusCode;
        this.data = data;
        this.code = "success";
        this.message = "요청에 성공했습니다";
    }

    private ResponseApi(int statusCode, String code, String message, T data) {
        this.statusCode = statusCode;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseApi<T> ok() {
        return new ResponseApi<>(HttpStatus.OK.value(), null);
    }

    public static <T> ResponseApi<T> ok(T data) {
        return new ResponseApi<>(HttpStatus.OK.value(), data);
    }

    public static <T> ResponseApi<T> created(HttpServletResponse response, T data) {
        response.setStatus(HttpStatus.CREATED.value());
        return new ResponseApi<>(HttpStatus.CREATED.value(), data);
    }

    public static <T> ResponseApi<T> nok(HttpStatusCode statusCode, String errorCode, String message) {
        return new ResponseApi<>(statusCode.value(), errorCode, message, null);
    }
}
