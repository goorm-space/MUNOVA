package com.space.munova.product.application.like.command.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.product.application.like.command.ProductLikeCommandService;
import com.space.munova.product.application.like.command.port.ProductLikeOutboxCommandPort;
import com.space.munova.product.application.product.command.event.ProductDeleteEvenForLikeDto;
import com.space.munova.product.application.product.command.port.OutboxCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductLikeEventListener {

    private final ProductLikeCommandService productLikeCommandService;
    private final ProductLikeOutboxCommandPort productLikeOutboxCommandPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeDelete(ProductDeleteEvenForLikeDto event) {
        try{
            if(event.isDeleted()) {
                productLikeCommandService.deleteProductLikeByProductIds(event.productId());
            }
        } catch(Exception e){
            /// 이벤트 저장실패시. 실패로 다시저장.
            productLikeOutboxCommandPort.deleteLikeFailedEvent(event);
        }

    }
}
