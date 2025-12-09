package com.space.munova.product.application.like.command.port;

import com.space.munova.product.application.product.command.event.ProductDeleteEvenForLikeDto;

public interface ProductLikeOutboxCommandPort {

    /// 아웃박스 실패 저장
    void deleteLikeFailedEvent(ProductDeleteEvenForLikeDto event);

}
