package com.space.munova.product.infra.mongo.command;

import com.space.munova.product.infra.mongo.ProductOutboxMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductOutBoxMongoCommandRepo extends MongoRepository<ProductOutboxMongoDocument, Long>, ProductMongoCommandRepoCustom {
}
