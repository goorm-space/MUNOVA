package com.space.munova.product.infra.mongo.command;

import com.space.munova.product.infra.mongo.ProductMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductMongoCommandRepo extends MongoRepository<ProductMongoDocument, Long>, ProductMongoCommandRepoCustom {
}
