package com.space.munova.product.infra.mysql.adepter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.application.product.query.port.ProductEsQuerySyncFailedPort;
import com.space.munova.product.application.product.query.port.ProductMongoQuerySyncFailedPort;
import com.space.munova.product.application.product.query.port.ProductMongoSyncPort;
import com.space.munova.product.domain.enums.EventType;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.product.infra.mysql.ProductOutbox;
import com.space.munova.product.infra.mysql.ProductOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/// 아웃박스에 실패 저장하는 어뎁터.
@RequiredArgsConstructor
@Component
@Transactional
public class MongoSyncFailedAdapter implements ProductMongoQuerySyncFailedPort {
    private final ProductOutboxRepository productOutboxRepository;
    private final ObjectMapper objectMapper;  // 벨류 json형식으로 변환을위한 객체


    @Override
    public void syncSaveMongoFailedEvent(ProductMongoDocument event) {

        saveFailedOutbox(EventType.SAVE_PRODUCT_SYNC_MONGO, event);
    }



    @Override
    public void syncDeleteMongoFailedEvent(ProductDocDeleteEventDto event) {
        saveFailedOutbox(EventType.DELETE_PRODUCT_SYNC_MONGO, event);
    }


    @Override
    public void syncUpdateFailedMongoEvent(ProductUpdateEventDto event) {
        saveFailedOutbox(EventType.UPDATE_PRODUCT_SYNC_MONGO, event);
    }



    /// =================================================================///



    private void saveFailedOutbox(EventType eventType, Object eventValue) {
        try {
            // JSON 변환
            String jsonValue = objectMapper.writeValueAsString(eventValue);

            ProductOutbox outbox = ProductOutbox.from(eventType, jsonValue);
            outbox.changeFailedStatus();

            productOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {

            throw new RuntimeException("Outbox 저장 실패", e);
        }
    }
}
