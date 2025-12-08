package com.space.munova.product.infra.elasticsearch.query.adapter;

import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.application.product.query.exception.ProductQueryException;
import com.space.munova.product.application.product.query.port.ProductEsSyncPort;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.elasticsearch.query.ProductEsSyncRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ProductEsSyncAdapter implements ProductEsSyncPort {

    private final ProductEsSyncRepo productEsSyncRepo;

    @Override
    public void syncDelete(ProductDocDeleteEventDto dto) {

        if(dto.isDeleted()) {
            try {
                productEsSyncRepo.deleteAllById(dto.productIds());
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                // Elasticsearch _delete_by_query can throw version_conflict when the doc is already gone; ignore and continue.
                log.warn("ES delete version conflict ignored. productIds={}", dto.productIds(), e);
            }
        }
    }

    @Override
    public void syncUpdate(ProductUpdateEventDto dto) {

        ProductEsDocument existingDoc = productEsSyncRepo.findById(dto.productId())
                .orElseThrow(() -> ProductQueryException.badRequestException(
                        "Elasticsearch 문서를 찾을 수 없습니다: " + dto.productId()));

        ProductEsDocument updatedDoc = ProductEsDocument.fromUpdate(
                existingDoc,  // 기존 문서
                dto.updatedMainImg(),  // 업데이트된 메인 이미지
                dto.savedDetailAndOptionInfoDto(), // 새로 추가된 옵션 정보
                dto.productName(),
                dto.price()
        );

        productEsSyncRepo.save(updatedDoc);
    }

    @Override
    public void syncSave(ProductEsDocument doc) {

        productEsSyncRepo.save(doc);
    }
}
