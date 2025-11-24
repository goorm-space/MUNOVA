package com.space.munova.velvetQ.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class VelvetQException extends BaseException {

    public VelvetQException(String code, String message, HttpStatusCode statusCode, String redirectUrl) {
        super(code, message, statusCode, redirectUrl);
    }

    public VelvetQException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static VelvetQException redirectWaitingQueueException(String redirectUrl) {
        return new VelvetQException("Q_REDIRECT", "대기열 대기 화면으로 이동", HttpStatus.PERMANENT_REDIRECT, redirectUrl);
    }

    public static VelvetQException failExternalCallException(String... detailMessage) {
        return new VelvetQException("Q_ERROR", "대기열 응답 실패", HttpStatus.INTERNAL_SERVER_ERROR, detailMessage);
    }
}
