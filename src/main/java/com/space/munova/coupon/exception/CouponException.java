package com.space.munova.coupon.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class CouponException extends BaseException {

    public CouponException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static CouponException invalidMinPaymentException(String... detailMessage) {
        return new CouponException("COUPON_01", "최소 주문 금액보다 주문가격이 낮습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static CouponException expiredException(String... detailMessage) {
        return new CouponException("COUPON_02", "만료된 쿠폰입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static CouponException soldOutException(String... detailMessage) {
        return new CouponException("COUPON_03", "쿠폰 재고가 소진되었습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static CouponException notFoundException(String... detailMessage) {
        return new CouponException("COUPON_04", "만료된 쿠폰이거나 등록되지 않은 쿠폰입니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static CouponException duplicateIssueException(String... detailMessage) {
        return new CouponException("COUPON_05", "이미 발급된 쿠폰입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static CouponException alreadyUsedException(String... detailMessage) {
        return new CouponException("COUPON_06", "이미 사용한 쿠폰입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static CouponException notPublishedException(String... detailMessage) {
        return new CouponException("COUPON_07", "아직 발행되지 않은 쿠폰입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }
}
