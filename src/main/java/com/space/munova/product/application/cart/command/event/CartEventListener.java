package com.space.munova.product.application.cart.command.event;

import com.space.munova.product.application.cart.command.CartCommandService;
import com.space.munova.product.application.cart.command.port.CartOutboxCommandPort;
import com.space.munova.product.application.product.command.event.ProductDeleteEventForCartDto;
import com.space.munova.product.application.product.command.port.OutboxCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventListener {

    private final CartCommandService cartCommandService;
    private final CartOutboxCommandPort cartOutboxCommandPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCartDeleted(ProductDeleteEventForCartDto event) {

        try{
            if(event.isDeleted()) {
                cartCommandService.deleteByProductDetailIds(event.productDetailIds());
            }
        } catch(Exception e){
            /// 이벤트 저장실패시. 실패로 다시저장.
            cartOutboxCommandPort.deleteCartFailedEvent(event);
        }

    }
}
