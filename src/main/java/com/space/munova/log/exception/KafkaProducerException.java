package com.space.munova.log.exception;

import org.springframework.http.HttpStatus;

public class KafkaProducerException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    private KafkaProducerException(String code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // DLQ 전송 실패
    public static KafkaProducerException dlqFailure(Throwable cause) {
        return new KafkaProducerException(
                "KAFKA_02",
                "Kafka DLQ 전송 실패",
                HttpStatus.INTERNAL_SERVER_ERROR,
                cause
        );
    }

    // File fallback 저장 실패
    public static KafkaProducerException fallbackFailure(Throwable cause) {
        return new KafkaProducerException(
                "KAFKA_03",
                "Kafka fallback 파일 저장 실패",
                HttpStatus.INTERNAL_SERVER_ERROR,
                cause
        );
    }
}