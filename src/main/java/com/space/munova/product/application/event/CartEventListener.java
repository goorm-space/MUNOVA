package com.space.munova.product.application.event;

import com.space.munova.product.application.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CartEventListener {

    private final CartService cartService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCartDeleted(ProductDeleteEventForCartDto event) {

        if(event.isDeleted()) {
            cartService.deleteByProductDetailIds(event.productDetailIds());
        }
    }
}
