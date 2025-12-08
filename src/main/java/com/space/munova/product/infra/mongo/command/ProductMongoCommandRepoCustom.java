package com.space.munova.product.infra.mongo.command;

import com.space.munova.product.infra.mongo.ProductOutboxMongoDocument;

public interface ProductMongoCommandRepoCustom {
    void saveProductOutBox(ProductOutboxMongoDocument productOutboxMongoDocument);
}
