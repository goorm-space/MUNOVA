package com.space.munova.core.exception;

import com.space.munova.core.config.ResponseApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class BaseExceptionHandler {

    /**
     * 도메인 예외 처리
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResponseApi<Object>> handleBaseException(BaseException ex) {
        String code = ex.getCode();
        String message = ex.getMessage();
        String detailMessage = ex.getDetailMessage();
        HttpStatusCode statusCode = ex.getStatusCode();

        String finalMessage = detailMessage != null ? message + " " + detailMessage : message;
        ResponseApi<Object> body = ResponseApi.nok(statusCode, code, finalMessage);

        log.error("{}: {}", statusCode, finalMessage);

        return ResponseEntity.status(statusCode).body(body);
    }

    /**
     * Security 예외처리
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseApi<Object> handleAuthenticationException(AuthenticationException ex) {
        log.error("{}: {}", HttpStatus.UNAUTHORIZED, ex.getMessage());

        return ResponseApi.nok(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", ex.getMessage());
    }

    /**
     * @Valid로 인한 예외 처리
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseApi<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("{}: {}", HttpStatus.BAD_REQUEST, errorMessage);

        return ResponseApi.nok(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", errorMessage);
    }

    /**
     * 기타 서버 예외처리
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseApi<Object> handleInternalServerException(Exception ex) {
        String errorMessage = "서버 내부 오류가 발생하였습니다.";
        log.error(errorMessage, ex);

        return ResponseApi.nok(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", errorMessage);
    }

}
