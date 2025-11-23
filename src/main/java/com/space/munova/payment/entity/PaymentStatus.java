package com.space.munova.payment.entity;

import com.space.munova.payment.exception.PaymentException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    READY("결제 준비"),
    IN_PROGRESS("결제 진행 중"),
    WAITING_FOR_DEPOSIT("입금 대기 중 (가상 계좌)"),
    DONE("결제 완료"),
    CANCELED("결제 취소"),
    PARTIAL_CANCELED("부분 취소"),
    ABORTED("결제 실패"),
    EXPIRED("결제 시간 만료");

    private final String description;

    public boolean isDone() {
        return this.equals(DONE);
    }

    public boolean isUpdatableStatus() {
        return this.equals(DONE) || this.equals(PARTIAL_CANCELED);
    }
}
