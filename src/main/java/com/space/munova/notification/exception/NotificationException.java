package com.space.munova.notification.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class NotificationException extends BaseException {

    public NotificationException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static NotificationException notfoundException(String... detailMessage) {
        return new NotificationException("NOTI_01", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }
}
