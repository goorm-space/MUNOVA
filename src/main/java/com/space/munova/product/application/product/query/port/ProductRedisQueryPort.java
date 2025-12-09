package com.space.munova.product.application.product.query.port;

public interface ProductRedisQueryPort {

    Integer findProductLikeCount(Long productId);

    Integer findProductViewCount(Long productId);
}
