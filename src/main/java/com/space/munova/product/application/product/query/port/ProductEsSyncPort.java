package com.space.munova.product.application.product.query.port;

import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.ProductMongoDocument;

public interface ProductEsSyncPort {
    void syncDelete(ProductDocDeleteEventDto dto);
    void syncUpdate(ProductUpdateEventDto dto);
    void syncSave(ProductEsDocument doc);
}
