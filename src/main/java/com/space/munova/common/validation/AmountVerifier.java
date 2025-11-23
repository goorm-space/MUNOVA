package com.space.munova.common.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmountVerifier {

    public static void verify(Long expectedAmount, Long actualAmount) {
        if (!expectedAmount.equals(actualAmount)) {
            throw new IllegalArgumentException(
                    String.format("요청금액: %d, 실제금액: %d",  expectedAmount, actualAmount)
            );
        }
    }
}
