package com.space.munova.payment.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class PaymentException extends BaseException {

    public PaymentException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static PaymentException amountMismatchException(String... detailMessage) {
        return new PaymentException("PAYMENT_01", "실제 결제 금액과 서버 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static PaymentException orderMismatchException(String... detailMessage) {
        return new PaymentException("PAYMENT_02", "해당 주문에 대한 결제 내역이 없습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static PaymentException tossApiCallFailedException(String... detailMessage) {
        return new PaymentException("TOSS_API_01", "TOSS API 응답을 실패했습니다.", HttpStatus.BAD_GATEWAY, detailMessage);
    }

    public static PaymentException illegalPaymentStateException(String... detailMessage) {
        return new PaymentException("PAYMENT_03", "승인된 결제만 취소/환불 정보를 업데이트 할 수 있습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }
}
