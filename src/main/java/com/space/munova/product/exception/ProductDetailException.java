package com.space.munova.product.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class ProductDetailException extends BaseException {


    public ProductDetailException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static ProductDetailException notFoundException(String... detailMessage) {
        return new ProductDetailException("PRODUCT_DETAIL_01", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ProductDetailException noStockException(String... detailMessage) {
        return new ProductDetailException("STOCK_01", "재고가 없습니다.", HttpStatus.CONFLICT, detailMessage);
    }

    public static ProductDetailException stockInsufficientException(String... detailMessage) {
        return new ProductDetailException("STOCK_02", "요청 수량이 재고보다 많습니다.", HttpStatus.CONFLICT, detailMessage);
    }
}
