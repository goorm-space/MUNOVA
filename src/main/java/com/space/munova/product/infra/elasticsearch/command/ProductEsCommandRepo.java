package com.space.munova.product.infra.elasticsearch.command;

import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.command.ProductMongoCommandRepo;
import com.space.munova.product.infra.mongo.command.ProductMongoCommandRepoCustom;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductEsCommandRepo extends ElasticsearchRepository<ProductEsDocument, Long>, ProductEsCommandRepoCustom {
}
