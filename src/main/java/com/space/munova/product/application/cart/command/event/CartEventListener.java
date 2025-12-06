package com.space.munova.product.application.cart.command.event;

import com.space.munova.product.application.cart.command.CartCommandService;
import com.space.munova.product.application.event.ProductDeleteEventForCartDto;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CartEventListener {

    private final CartCommandService cartCommandService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCartDeleted(ProductDeleteEventForCartDto event) {

        if(event.isDeleted()) {
            cartCommandService.deleteByProductDetailIds(event.productDetailIds());
        }
    }
}
