package com.space.munova.product.infra.mongo.query.adapter;

import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.application.product.query.exception.ProductQueryException;
import com.space.munova.product.application.product.query.port.ProductMongoQuerySyncFailedPort;
import com.space.munova.product.application.product.query.port.ProductMongoSyncPort;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.product.infra.mongo.query.ProductMongoSyncRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


///  몽고 싱크 포트
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoSyncAdapter implements ProductMongoSyncPort {

    private final ProductMongoSyncRepo productMongoSyncRepo;

    @Override
    public void syncDelete(ProductDocDeleteEventDto dto) {

       if (dto.isDeleted()) {
           productMongoSyncRepo.deleteAllById(dto.productIds());
       }
    }

    @Override
    public void syncUpdate(ProductUpdateEventDto dto) {
        log.info("Starting Mongo Sync Update for productId: {}", dto.productId());
        ProductMongoDocument existingDoc = productMongoSyncRepo.findById(dto.productId())
                .orElseThrow(() -> ProductQueryException.badRequestException("MongoDB 문서를 찾을 수 없습니다: " + dto.productId()));

        //  업데이트된 문서 생성
        ProductMongoDocument updatedDoc = ProductMongoDocument.fromUpdate(
                existingDoc,  // 기존 문서 (brand, category 등 유지)
                dto.productId(),
                dto.productName(),
                dto.info(),
                dto.price(),
                dto.updatedMainImg(),
                dto.addSideImages(),
                dto.removeSideImages(),
                dto.savedDetailAndOptionInfoDto(),
                dto.updateQuantityDtos(),
                dto.deleteDetailIds()
        );
        log.info("Saving updated Mongo doc: {}", updatedDoc);
        //  저장 upsert
        productMongoSyncRepo.save(updatedDoc);
    }

    @Override
    public void syncSave(ProductMongoDocument doc) {


        productMongoSyncRepo.save(doc);
    }

}
