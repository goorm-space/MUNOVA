package com.space.munova.product.application.exception;


import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public final class ProductException extends BaseException {


    public ProductException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }


    public static ProductException notFoundProductException(String... detailMessage) {
        return new ProductException("PRODUCT_01", "유효하지 않은 상품입니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ProductException notFoundCategoryException(String... detailMessage) {
        return new ProductException("PRODUCT_02", "유효하지 않은 상품 카테고리 입니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ProductException notFoundBrandException(String... detailMessage) {
        return new ProductException("PRODUCT_03", "유효하지 않은 브랜드 입니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ProductException badRequestException(String... detailMessage) {
        return new ProductException("PRODUCT_05", "유효하지 요청 입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static ProductException unauthorizedAccessException(String... detailMessage) {
        return new ProductException("PRODUCT_05", "권한 없는 접근 입니다.", HttpStatus.FORBIDDEN, detailMessage);
    }

    public static ProductException notFoundProductDetailExeption(String... detailMessage) {
        return new ProductException("PRODUCT_06", "유효하지 않은 상품 정보 입니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

}
