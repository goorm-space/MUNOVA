package com.space.munova.product.infra.elasticsearch.query;

import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductEsSyncRepo extends ElasticsearchRepository<ProductEsDocument, Long> {
}
