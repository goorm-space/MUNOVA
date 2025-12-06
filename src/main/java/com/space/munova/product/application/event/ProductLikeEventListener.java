package com.space.munova.product.application.event;

import com.space.munova.product.application.like.command.ProductLikeCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductLikeEventListener {

    private final ProductLikeCommandService productLikeCommandService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeDelete(ProductDeleteEvenForLikeDto event) {
        if(event.isDeleted()) {
            productLikeCommandService.deleteProductLikeByProductIds(event.productId());
        }
    }
}
