package com.space.munova.product.application.cart.command.port;

import com.space.munova.product.application.product.command.event.ProductDeleteEventForCartDto;

public interface CartOutboxCommandPort {

    void deleteCartFailedEvent(ProductDeleteEventForCartDto event);
}
