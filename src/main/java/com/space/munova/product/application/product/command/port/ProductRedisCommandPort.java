package com.space.munova.product.application.product.command.port;

public interface ProductRedisCommandPort {

    Long updateViewCount(Long productId, int input);
}
