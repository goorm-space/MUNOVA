package com.space.munova.product.application.like.command.port;

import com.space.munova.product.application.product.command.event.ProductLikeEventDto;

public interface ProductLikeMongoCommandPort {
    void save(ProductLikeEventDto dto);
}
