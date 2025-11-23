package com.space.munova.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelStatus {
    DONE("결제 취소 완료");

    private final String description;

    public boolean isDone() {
        return this.equals(DONE);
    }
}
