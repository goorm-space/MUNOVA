package com.space.munova.product.infra.batch;

import com.space.munova.product.application.product.command.port.OutboxCommandPort;
import com.space.munova.product.application.product.query.port.ProductMongoSyncPort;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.infra.batch.dto.ProductStatsSyncDto;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.elasticsearch.query.ProductEsSyncRepo;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.product.infra.mongo.query.ProductMongoSyncRepo;
import com.space.munova.product.infra.mysql.adepter.OutboxCommandAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductStatsWriter implements ItemWriter<ProductStatsSyncDto> {

    private final ProductRepository productRepository;
    private final ProductMongoSyncRepo productMongoSyncRepo;
    private final ProductEsSyncRepo productEsSyncRepo;
    private final OutboxCommandPort outboxCommandPort;
    @Override
    @Transactional(rollbackFor = Exception.class) /// 예외 발생시 mysql 롤백
    public void write(Chunk<? extends ProductStatsSyncDto> chunk) throws Exception {

        List<ProductStatsSyncDto> items = (List<ProductStatsSyncDto>) chunk.getItems();

        for (ProductStatsSyncDto dto : items) {

            ProductMongoDocument mongoDocBefore = null;
            ProductEsDocument esDocBefore = null;

            try {
                updateMysql(dto);

                mongoDocBefore = productMongoSyncRepo.findById(dto.getProductId()).orElse(null);
                updateMongoDoc(dto);

                esDocBefore = productEsSyncRepo.findById(dto.getProductId()).orElse(null);
                updateEsDoc(dto);

            } catch (Exception e) { ///  예외 발생시  아웃박스 패턴으로 기존 몽고, 엘라로 싱크 세이브 메시지를 저장하여
                                    /// 기존 문서 다시저장.

                if (mongoDocBefore != null) {

                    outboxCommandPort.syncSaveMongoEvent(mongoDocBefore);
                }

                if (esDocBefore != null) {

                    outboxCommandPort.syncSaveEsEvent(esDocBefore);
                }

                throw new RuntimeException("상품 통계 동기화 실패: productId=" + dto.getProductId(), e);
            }
        }
    }

    private void updateMysql(ProductStatsSyncDto dto) {
        productRepository.updateProductStats(
                dto.getProductId(),
                dto.getLikeCount(),
                dto.getViewCount(),
                dto.getSalesCount()
        );
    }

    private void updateMongoDoc(ProductStatsSyncDto dto) {
        ProductMongoDocument doc = productMongoSyncRepo.findById(dto.getProductId())
                .orElse(null);

        if (doc != null) {
            doc.updateStats(dto.getLikeCount(), dto.getViewCount(), dto.getSalesCount());
            productMongoSyncRepo.save(doc);
        }
    }

    private void updateEsDoc(ProductStatsSyncDto dto) {
        ProductEsDocument doc = productEsSyncRepo.findById(dto.getProductId())
                .orElse(null);

        if (doc != null) {
            doc.updateStats(dto.getLikeCount(), dto.getViewCount(), dto.getSalesCount());
            productEsSyncRepo.save(doc);
        }
    }
}
