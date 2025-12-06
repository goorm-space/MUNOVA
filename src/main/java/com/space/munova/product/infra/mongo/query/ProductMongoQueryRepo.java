package com.space.munova.product.infra.mongo.query;


import com.space.munova.product.infra.mongo.ProductMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductMongoQueryRepo extends MongoRepository<ProductMongoDocument, Long>, ProductMongoQueryRepoCustom {


}
