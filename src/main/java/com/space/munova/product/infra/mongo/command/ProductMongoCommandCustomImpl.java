package com.space.munova.product.infra.mongo.command;

import com.space.munova.product.infra.mongo.ProductOutboxMongoDocument;
import org.springframework.stereotype.Repository;

@Repository
public class ProductMongoCommandCustomImpl implements ProductMongoCommandRepoCustom{
    @Override
    public void saveProductOutBox(ProductOutboxMongoDocument productOutboxMongoDocument) {

    }
}
