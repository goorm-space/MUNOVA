package com.space.munova.recommend.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class RecommendException extends BaseException {
    public RecommendException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static RecommendException productNotFound(String... detailMessage) {
        return new RecommendException("RECOMMEND_01", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static RecommendException categoryNotFound(String... detailMessage) {
        return new RecommendException("RECOMMEND_02", "카테고리가 없는 상품은 추천할 수 없습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static RecommendException targetProductNotFound(String... detailMessage) {
        return new RecommendException("RECOMMEND_03", "추천 대상 상품이 존재하지 않습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }
}
