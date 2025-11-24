package com.space.munova.payment.client.exception;

import com.space.munova.core.exception.BaseException;
import com.space.munova.payment.exception.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class TossClientException extends BaseException {

    public TossClientException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static TossClientException toJsonException(String... detailMessage) {
        return new TossClientException("TOSS_CLIENT_01", "Toss API 요청/응답 직렬화를 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, detailMessage);
    }

    public static TossClientException networkIoException(String... detailMessage) {
        return new TossClientException("TOSS_CLIENT_02", "Toss API 통신 중 네트워크 I/O 오류가 발생했습니다.", HttpStatus.SERVICE_UNAVAILABLE, detailMessage);
    }

    public static TossClientException threadInterruptedError(String... detailMessage) {
        // 애플리케이션 내부에서 예상치 못한 문제가 발생했음을 나타내는 500 사용
        return new TossClientException("TOSS_CLIENT_03", "Toss API 통신 중 쓰레드가 중단되었습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR, detailMessage);
    }

    public static TossClientException apiCallFailedException(String... detailMessage) {
        return new TossClientException("TOSS_CLIENT_04", "TOSS API 응답을 실패했습니다.", HttpStatus.BAD_GATEWAY, detailMessage);
    }

}
