package com.space.munova.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OutboxStatus {
    PENDING ("PENDING"),      // 발송 대기
    PUBLISHED ("PUBLISHED"),    // 발송 완료
    FAILED ("FAILED");   // 발송 실패 (재시도 필요)

    private final String value;
}