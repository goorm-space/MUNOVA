package com.space.munova.product.application.product.command.event;

import com.space.munova.product.application.product.command.ProductCommandFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductEventListener {

//    private final ProductCommandFacadeService productCommandFacadeService;
//
//    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleProductLikeEvent(ProductLikeEventDto event) {
//
//        ///  딜리티드가 트루일경우 product LikeCount --
//        ///  딜리티드가 펄스 일경우 Product LikeCount ++
//        if(event.isDeleted()) {
//            productCommandFacadeService.minusLikeCountInProductIds(event.productId());
//        } else {
//
//            productCommandFacadeService.plusLikeCountByProductId(event.productId());
//        }
//    }
}
