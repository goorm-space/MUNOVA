package com.space.munova.product.application.product.query.port;

import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.infra.mongo.ProductMongoDocument;

public interface ProductMongoQuerySyncFailedPort {
    void syncSaveMongoFailedEvent(ProductMongoDocument event);
    void syncDeleteMongoFailedEvent(ProductDocDeleteEventDto event);
    void syncUpdateFailedMongoEvent(ProductUpdateEventDto event);

}
