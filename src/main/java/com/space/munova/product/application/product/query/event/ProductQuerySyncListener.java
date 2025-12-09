package com.space.munova.product.application.product.query.event;

import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.application.product.query.port.ProductEsQuerySyncFailedPort;
import com.space.munova.product.application.product.query.port.ProductEsSyncPort;
import com.space.munova.product.application.product.query.port.ProductMongoQuerySyncFailedPort;
import com.space.munova.product.application.product.query.port.ProductMongoSyncPort;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductQuerySyncListener {

    ///  아웃박스 실패 저장 포트
    private final ProductEsQuerySyncFailedPort productEsQuerySyncFailedPort;
    private final ProductMongoQuerySyncFailedPort productMongoQuerySyncFailedPort;

    /// 몽고 엘라 저장 포트
    private final ProductMongoSyncPort productMongoSyncPort;
    private final ProductEsSyncPort productEsSyncPort;

    /// 몽고 저장
    @Async
    @TransactionalEventListener
    public void synSaveProductMongo(ProductMongoDocument event) {
        try {
            productMongoSyncPort.syncSave(event);
        } catch (Exception e) {
            log.error("Mongo Save Failed", e);
            productMongoQuerySyncFailedPort.syncSaveMongoFailedEvent(event);
        }
    }

    /// 엘라 저장.
    @Async
    @TransactionalEventListener
    public void syncSaveProductEs(ProductEsDocument event) {
        try {

            productEsSyncPort.syncSave(event);
        } catch (Exception e) {
            log.error("ES Save Failed", e);
            productEsQuerySyncFailedPort.syncSaveEsFailedEvent(event);
        }
    }

    /// 몽고 업데이트
    @Async
    @TransactionalEventListener
    public void syncUpdateProductMongo(ProductUpdateEventDto event) {
        try {
            log.info("Mongo Update Event Received: {}", event);
            productMongoSyncPort.syncUpdate(event);
        } catch (Exception e) {
            log.error("Mongo Update Failed", e);
            productMongoQuerySyncFailedPort.syncUpdateFailedMongoEvent(event);
        }
    }

    /// 엘라 업데이트
    @Async
    @TransactionalEventListener
    public void syncUpdateProductEs(ProductUpdateEventDto event) {
        try {
            log.info("ES Update Event Received: {}", event);
            productEsSyncPort.syncUpdate(event);
        } catch (Exception e) {
            log.error("ES Update Failed", e);
            productEsQuerySyncFailedPort.syncUpdateEsFailedEvent(event);
        }
    }

    /// 몽고 삭제
    @Async
    @TransactionalEventListener
    public void syncDeleteProductMongo(ProductDocDeleteEventDto event) {
        try {

            productMongoSyncPort.syncDelete(event);
        } catch (Exception e) {
            log.error("Mongo Delete Failed", e);
            productMongoQuerySyncFailedPort.syncDeleteMongoFailedEvent(event);
        }
    }

    /// 엘라 삭제
    @Async
    @TransactionalEventListener
    public void syncDeleteProductEs(ProductDocDeleteEventDto event) {
        try {

            productEsSyncPort.syncDelete(event);
        } catch (Exception e) {
            log.error("ES Delete Failed", e);
            productEsQuerySyncFailedPort.syncDeleteEsFailedEvent(event);
        }
    }
}
