package com.space.munova.product.application.product.query.port;

import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;

public interface ProductEsQuerySyncFailedPort {

    void syncSaveEsFailedEvent(ProductEsDocument event);
    void syncDeleteEsFailedEvent(ProductDocDeleteEventDto event);
    void syncUpdateEsFailedEvent(ProductUpdateEventDto event);

}
