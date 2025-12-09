package com.space.munova.product.application.like.command.port;

public interface ProductLikeCommandPort {
    Long like(Long productId, int input);
    Long dislike(Long productId, int input);
}
